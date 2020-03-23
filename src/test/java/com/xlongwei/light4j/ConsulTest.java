package com.xlongwei.light4j;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.xnio.OptionMap;

import com.networknt.client.Http2Client;
import com.networknt.cluster.Cluster;
import com.networknt.exception.ClientException;
import com.networknt.server.Server;
import com.networknt.service.SingletonServiceFactory;

import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import lombok.extern.slf4j.Slf4j;

/**
 * consul test
 * @author xlongwei
 *
 */
@Slf4j
public class ConsulTest {
    static Cluster cluster = SingletonServiceFactory.getBean(Cluster.class);
    static String path = "/service/datetime";
    static String tag = Server.getServerConfig().getEnvironment();
    static Http2Client client = Http2Client.getInstance();
    static ClientConnection connection;
    static Map<String, ClientConnection> connections = new ConcurrentHashMap<>();
    
	@Test public void discover() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        if(connection == null || !connection.isOpen()) {
            try {
                String apidHost = cluster.serviceToUrl("https", "com.xlongwei.light4j", tag, null);
                connection = client.connect(new URI(apidHost), Http2Client.WORKER, client.getDefaultXnioSsl(), Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
            } catch (Exception e) {
                log.error("Exeption:", e);
                throw new ClientException(e);
            }
        }
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath(path);
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            connection.sendRequest(request, client.createClientCallback(reference, latch));
            latch.await();
            String datetime = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
            log.info("datetime: {}", datetime);
        } catch (Exception e) {
            log.error("Exception:", e);
            throw new ClientException(e);
        }
	}
	
	@Test public void discovers() throws Exception {
		int count = 5;
		for(int i=0; i<count; i++) {
			String service = cluster.serviceToUrl("https", "com.xlongwei.light4j", tag, null);
			log.info("{} ==> {}", i, service);
			ClientConnection connection = connections.get(service);
	        if(connection == null || !connection.isOpen()) {
	            try {
	                connection = client.connect(new URI(service), Http2Client.WORKER, client.getDefaultXnioSsl(), Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
	                connections.put(service, connection);
	            } catch (Exception e) {
	            	log.info("fail to get connection, i=[{}]", i);
	                continue;
	            }
	        }
	        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
	        final CountDownLatch latch = new CountDownLatch(1);
	        try {
	            ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath(path);
	            connection.sendRequest(request, client.createClientCallback(reference, latch));
	            latch.await();
	            int statusCode = reference.get().getResponseCode();
	            if(statusCode >= 300){
	                throw new Exception("Failed to call API D: " + statusCode);
	            }
	            String datetime = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
	            log.info("{}", datetime);
	        } catch (Exception e) {
	        	log.info("fail to request service, i=[{}]", i);
	            continue;
	        }
		}
	}
}
