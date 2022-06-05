/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.server;

import java.net.InetAddress;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.xnio.Options;
import org.xnio.SslClientAuthMode;

import com.networknt.common.SecretConstants;
import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.HandlerProvider;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.handler.OrchestrationHandler;
import com.networknt.registry.Registry;
import com.networknt.registry.URL;
import com.networknt.registry.URLImpl;
import com.networknt.service.SingletonServiceFactory;
import com.networknt.switcher.SwitcherUtil;
import com.networknt.utility.Constants;
import com.networknt.utility.TlsUtil;
import com.networknt.utility.Util;
import com.xlongwei.light4j.apijson.DemoApplication;
import com.xlongwei.light4j.util.TaskUtil;

import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.GracefulShutdownHandler;

/**
 * This is the entry point of the framework. It wrapped Undertow Core HTTP
 * server and controls the lifecycle of the server. It also orchestrate
 * different types of plugins and wire them in at the right location.
 *
 * @author Steve Hu
 */
public class Servers {

    static final Logger logger = LoggerFactory.getLogger(Servers.class);
    public static final String SERVER_CONFIG_NAME = "server";
    public static final String SECRET_CONFIG_NAME = "secret";
    public static final String STARTUP_CONFIG_NAME = "startup";
    public static final String CONFIG_LOADER_CLASS = "configLoaderClass";
    public static final String[] STATUS_CONFIG_NAME = {"status", "app-status"};
    
    public static final String ENV_PROPERTY_KEY = "environment";

    static final String STATUS_HOST_IP = "STATUS_HOST_IP";

    static final String SID = "sId";
    // the bound port for the server. For metrics and other queries.
    public static int currentPort;
    // the bound ip for the server. For metrics and other queries
    public static String currentAddress;

    public static final ServerConfig config = getServerConfig();

    public final static TrustManager[] TRUST_ALL_CERTS = new X509TrustManager[]{new DummyTrustManager()};
    /** a list of service ids populated by startup hooks that want to register to the service registry */
    public static final List<String> serviceIds = new ArrayList<>();
    /** a list of service urls kept in memory so that they can be unregistered during server shutdown */
    public static final List<URL> serviceUrls = new ArrayList<>();

    static protected boolean shutdownRequested = false;
    static Undertow server = null;
    static Registry registry;
    static SSLContext sslContext;

    static GracefulShutdownHandler gracefulShutdownHandler;

    public static void main(final String[] args) {
        init();
    }

    public static void init() {
        logger.info("server starts");
        // setup system property to redirect undertow logs to slf4j/logback.
        System.setProperty("org.jboss.logging.provider", "slf4j");

        try {

            loadConfigs();

            // this will make sure that all log statement will have serviceId
            MDC.put(SID, config.getServiceId());

            // merge status.yml and app-status.yml if app-status.yml is provided
            mergeStatusConfig();

            start();
        } catch (RuntimeException e) {
            // Handle any exception encountered during server start-up
            logger.error("Server is not operational! Failed with exception", e);
            System.out.println("Failed to start server:" + e.getMessage());
            // send a graceful system shutdown
            System.exit(1);
        }
    }

    /**
     * Locate the Config Loader class, instantiate it and then call init() method on it.
     * Uses DefaultConfigLoader if startup.yml is missing or configLoaderClass is missing in startup.yml
     */
    public static void loadConfigs(){
        IConfigLoader configLoader;
        Map<String, Object> startupConfig = Config.getInstance().getJsonMapConfig(STARTUP_CONFIG_NAME);
        if(startupConfig ==null || startupConfig.get(CONFIG_LOADER_CLASS) ==null){
            configLoader = new DefaultConfigLoader();
        }else{
            try {
                Class<?> clazz = Class.forName((String) startupConfig.get(CONFIG_LOADER_CLASS));
                configLoader = (IConfigLoader) clazz.getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("configLoaderClass mentioned in startup.yml could not be found or constructed", e);
            }
        }
        configLoader.init();
    }

    public static void start() {
        // add shutdown hook here.
        addDaemonShutdownHook();

        // add startup hooks here.
        StartupHookProvider[] startupHookProviders = SingletonServiceFactory.getBeans(StartupHookProvider.class);
        if (startupHookProviders != null) {
			Arrays.stream(startupHookProviders).forEach(StartupHookProvider::onStartup);
		}

        // For backwards compatibility, check if a handler.yml has been included. If
        // not, default to original configuration.
        if (Handler.config == null || !Handler.config.isEnabled()) {
            HttpHandler handler = middlewareInit();

            // register the graceful shutdown handler
            gracefulShutdownHandler = new GracefulShutdownHandler(handler);
        } else {
            // initialize the handlers, chains and paths
            Handler.init();

            // register the graceful shutdown handler
            gracefulShutdownHandler = new GracefulShutdownHandler(new OrchestrationHandler());
        }

        //bind http or https port
        bind(gracefulShutdownHandler, -1);
        
        //start APIJSON
    	DemoApplication.start();
    }

    private static HttpHandler middlewareInit() {
        HttpHandler handler = null;

        // API routing handler or others handler implemented by application developer.
        HandlerProvider handlerProvider = SingletonServiceFactory.getBean(HandlerProvider.class);
        if (handlerProvider != null) {
            handler = handlerProvider.getHandler();
        }
        if (handler == null) {
            logger.error("Unable to start the server - no route handler provider available in service.yml");
            throw new RuntimeException(
                    "Unable to start the server - no route handler provider available in service.yml");
        }
        // Middleware Handlers plugged into the handler chain.
        MiddlewareHandler[] middlewareHandlers = SingletonServiceFactory.getBeans(MiddlewareHandler.class);
        if (middlewareHandlers != null) {
            for (int i = middlewareHandlers.length - 1; i >= 0; i--) {
                logger.info("Plugin: " + middlewareHandlers[i].getClass().getName());
                if (middlewareHandlers[i].isEnabled()) {
                    handler = middlewareHandlers[i].setNext(handler);
                    middlewareHandlers[i].register();
                }
            }
        }
        return handler;
    }

    /**
     * Method used to initialize server options. If the user has configured a valid server option,
     * load it into the server configuration, otherwise use the default value
     */
    private static void serverOptionInit() {
        Map<String, Object> mapConfig = Config.getInstance().getJsonMapConfigNoCache(SERVER_CONFIG_NAME);
        ServerOption.serverOptionInit(mapConfig, getServerConfig());
    }

    static private boolean bind(HttpHandler handler, int port) {
        ServerConfig serverConfig = getServerConfig();
        try {
            Undertow.Builder builder = Undertow.builder();
            if (serverConfig.enableHttps) {
            	port = serverConfig.getHttpsPort();
                sslContext = createSSLContext();
                builder.addHttpsListener(port, serverConfig.getIp(), sslContext);
            }
            if (serverConfig.enableHttp) {
            	port = serverConfig.getHttpPort();
                builder.addHttpListener(serverConfig.getHttpPort(), serverConfig.getIp());
            }
            if(!serverConfig.enableHttps && !serverConfig.enableHttp) {
                throw new RuntimeException("Unable to start the server as both http and https are disabled in server.yml");
            }
            if (serverConfig.enableHttp2) {
                builder.setServerOption(UndertowOptions.ENABLE_HTTP2, true);
            }
            if (serverConfig.isEnableTwoWayTls()) {
               builder.setSocketOption(Options.SSL_CLIENT_AUTH_MODE, SslClientAuthMode.REQUIRED);
            }
            serverOptionInit();
            server = builder.setBufferSize(serverConfig.getBufferSize()).setIoThreads(serverConfig.getIoThreads())
                    .setSocketOption(Options.BACKLOG, serverConfig.getBacklog())
                    .setServerOption(UndertowOptions.ALWAYS_SET_KEEP_ALIVE, true)
                    .setServerOption(UndertowOptions.ALWAYS_SET_DATE, serverConfig.isAlwaysSetDate())
                    .setServerOption(UndertowOptions.RECORD_REQUEST_START_TIME, true)
                    .setServerOption(UndertowOptions.ALLOW_UNESCAPED_CHARACTERS_IN_URL, serverConfig.isAllowUnescapedCharactersInUrl())
                    .setHandler(handler).setWorkerThreads(serverConfig.getWorkerThreads()).build();
            server.start();
            System.out.println("HOST IP " + System.getenv(STATUS_HOST_IP));
        } catch (Exception e) {
            System.out.println("Failed to bind to port " + port + ". Trying " + ++port);
            if (logger.isInfoEnabled()) {
				logger.info("Failed to bind to port " + port + ". Trying " + ++port);
			}
            return false;
        }
        if (serverConfig.enableRegistry) {
            //serviceUrls = new ArrayList<>();
            serviceUrls.add(register(serverConfig.getServiceId(), port));
            if(serviceIds.size() > 0) {
                for(String id: serviceIds) {
                    serviceUrls.add(register(id, port));
                }
            }
            SwitcherUtil.setSwitcherValue(Constants.REGISTRY_HEARTBEAT_SWITCHER, true);
            if (logger.isInfoEnabled()) {
				logger.info("Registry heart beat switcher is on");
			}
        }
        if (serverConfig.enableHttp) {
        	port = serverConfig.getHttpPort();
            System.out.println("Http Server started on ip:" + serverConfig.getIp() + " Port:" + port);
            if (logger.isInfoEnabled()) {
				logger.info("Http Server started on ip:" + serverConfig.getIp() + " Port:" + port);
			}
        } else {
            System.out.println("Http port disabled.");
            if (logger.isInfoEnabled()) {
				logger.info("Http port disabled.");
			}
        }
        if (serverConfig.enableHttps) {
        	port = serverConfig.getHttpsPort();
            System.out.println("Https Server started on ip:" + serverConfig.getIp() + " Port:" + port);
            if (logger.isInfoEnabled()) {
				logger.info("Https Server started on ip:" + serverConfig.getIp() + " Port:" + port);
			}
        } else {
            System.out.println("Https port disabled.");
            if (logger.isInfoEnabled()) {
				logger.info("Https port disabled.");
			}
        }
        // at this moment, the port number is bound. save it for later queries
        currentPort = port;
        return true;
    }

    public static void stop() {
        if (server != null) {
			server.stop();
		}
    }

    public static void shutdown() {
        // need to unregister the service
        if (getServerConfig().enableRegistry && registry != null && serviceUrls != null) {
            for(URL serviceUrl: serviceUrls) {
                registry.unregister(serviceUrl);
                // Please don't remove the following line. When server is killed, the logback won't work anymore.
                // Even debugger won't reach this point; however, the logic is executed successfully here.
                System.out.println("unregister serviceUrl " + serviceUrl);
                if (logger.isInfoEnabled()) {
					logger.info("unregister serviceUrl " + serviceUrl);
				}
            }
        }

        if (gracefulShutdownHandler != null) {
            logger.info("Starting graceful shutdown.");
            gracefulShutdownHandler.shutdown();
            try {
                gracefulShutdownHandler.awaitShutdown(TimeUnit.SECONDS.toMillis(60));
            } catch (Exception e) {
                logger.error("Error occurred while waiting for pending requests to complete.", e);
            }
            logger.info("Graceful shutdown complete.");
        }

        ShutdownHookProvider[] shutdownHookProviders = SingletonServiceFactory.getBeans(ShutdownHookProvider.class);
        if (shutdownHookProviders != null) {
			Arrays.stream(shutdownHookProviders).forEach(ShutdownHookProvider::onShutdown);
		}

        stop();
        logger.info("Cleaning up before server shutdown");
    }

    static protected void addDaemonShutdownHook() {
        TaskUtil.addShutdownHook((Runnable)Servers::shutdown);
    }

    protected static KeyStore loadKeyStore() {
        Map<String, Object> secretConfig = Config.getInstance().getJsonMapConfig(SECRET_CONFIG_NAME);

        String name = getServerConfig().getKeystoreName();
        char[] password = ((String) secretConfig.get(SecretConstants.SERVER_KEYSTORE_PASS)).toCharArray();
        return TlsUtil.loadKeyStore(name, password);
    }

    protected static KeyStore loadTrustStore() {
        Map<String, Object> secretConfig = Config.getInstance().getJsonMapConfig(SECRET_CONFIG_NAME);

        String name = getServerConfig().getTruststoreName();
        char[] password = ((String) secretConfig.get(SecretConstants.SERVER_TRUSTSTORE_PASS)).toCharArray();
        return TlsUtil.loadTrustStore(name, password);
    }

    private static TrustManager[] buildTrustManagers(final KeyStore trustStore) {
        TrustManager[] trustManagers = null;
        if (trustStore != null) {
            try {
                TrustManagerFactory trustManagerFactory = TrustManagerFactory
                        .getInstance(KeyManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(trustStore);
                trustManagers = trustManagerFactory.getTrustManagers();
            } catch (NoSuchAlgorithmException | KeyStoreException e) {
                logger.error("Unable to initialise TrustManager[]", e);
                throw new RuntimeException("Unable to initialise TrustManager[]", e);
            }
        } else {
            // Mutual Tls is disabled, trust all the certs
            trustManagers = TRUST_ALL_CERTS;
        }
        return trustManagers;
    }

    private static KeyManager[] buildKeyManagers(final KeyStore keyStore, char[] keyPass) {
        KeyManager[] keyManagers;
        try {
            KeyManagerFactory keyManagerFactory = KeyManagerFactory
                    .getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keyPass);
            keyManagers = keyManagerFactory.getKeyManagers();
        } catch (NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException e) {
            logger.error("Unable to initialise KeyManager[]", e);
            throw new RuntimeException("Unable to initialise KeyManager[]", e);
        }
        return keyManagers;
    }

    private static SSLContext createSSLContext() throws RuntimeException {
        Map<String, Object> secretConfig = Config.getInstance().getJsonMapConfig(SECRET_CONFIG_NAME);

        try {
            KeyManager[] keyManagers = buildKeyManagers(loadKeyStore(),
                    ((String) secretConfig.get(SecretConstants.SERVER_KEY_PASS)).toCharArray());
            TrustManager[] trustManagers;
            if (getServerConfig().isEnableTwoWayTls()) {
                trustManagers = buildTrustManagers(loadTrustStore());
            } else {
                trustManagers = buildTrustManagers(null);
            }

            SSLContext sslContext;
            sslContext = SSLContext.getInstance("TLSv1");
            sslContext.init(keyManagers, trustManagers, null);
            return sslContext;
        } catch (Exception e) {
            logger.error("Unable to create SSLContext", e);
            throw new RuntimeException("Unable to create SSLContext", e);
        }
    }

    protected static void mergeStatusConfig() {
        Map<String, Object> appStatusConfig = Config.getInstance().getJsonMapConfigNoCache(STATUS_CONFIG_NAME[1]);
        if (appStatusConfig == null) {
            return;
        }
        Map<String, Object> statusConfig = Config.getInstance().getJsonMapConfig(STATUS_CONFIG_NAME[0]);
        // clone the default status config key set
        Set<String> duplicatedStatusSet = new HashSet<>(statusConfig.keySet());
        duplicatedStatusSet.retainAll(appStatusConfig.keySet());
        if (!duplicatedStatusSet.isEmpty()) {
            logger.error("The status code(s): " + duplicatedStatusSet.toString() + " is already in use by light-4j and cannot be overwritten," +
                    " please change to another status code in app-status.yml if necessary.");
            throw new RuntimeException("The status code(s): " + duplicatedStatusSet.toString() + " in status.yml and app-status.yml are duplicated.");
        }
        statusConfig.putAll(appStatusConfig);
    }

    public static ServerConfig getServerConfig(){
        return (ServerConfig) Config.getInstance().getJsonObjectConfig(SERVER_CONFIG_NAME,
                ServerConfig.class);
    }

    /**
     * Register the service to the Consul or other service registry. Make it as a separate static method so that it
     * can be called from light-hybrid-4j to register individual service.
     *
     * @param serviceId Service Id that is registered
     * @param port Port number of the service
     * @return URL
     */
    @SuppressWarnings({"rawtypes","unchecked"})
	public static URL register(String serviceId, int port) {
        try {
            registry = SingletonServiceFactory.getBean(Registry.class);
            if (registry == null) {
				throw new RuntimeException("Could not find registry instance in service map");
			}
            // in kubernetes pod, the hostIP is passed in as STATUS_HOST_IP environment
            // variable. If this is null
            // then get the current server IP as it is not running in Kubernetes.
            String ipAddress = System.getenv(STATUS_HOST_IP);
            logger.info("Registry IP from STATUS_HOST_IP is " + ipAddress);
            if (ipAddress == null) {
                InetAddress inetAddress = Util.getInetAddress();
                ipAddress = inetAddress.getHostAddress();
                logger.info("Could not find IP from STATUS_HOST_IP, use the InetAddress " + ipAddress);
            }
            currentAddress = ipAddress;
            ServerConfig serverConfig = getServerConfig();
            Map parameters = new HashMap<>(4);
            if (serverConfig.getEnvironment() != null) {
				parameters.put(ENV_PROPERTY_KEY, serverConfig.getEnvironment());
			}
            URL serviceUrl = new URLImpl("light", ipAddress, port, serviceId, parameters);
            if (logger.isInfoEnabled()) {
				logger.info("register service: " + serviceUrl.toFullStr());
			}
            registry.register(serviceUrl);
            return serviceUrl;
            // handle the registration exception separately to eliminate confusion
        } catch (Exception e) {
            System.out.println("Failed to register service, the server stopped.");
            if (logger.isInfoEnabled()) {
				logger.info("Failed to register service, the server stopped.", e);
			}
            throw new RuntimeException(e.getMessage());
        }

    }
}
