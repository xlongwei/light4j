package com.xlongwei.light4j;

import com.networknt.server.Server;
import com.networknt.server.ServerConfig;

/**
 * listen both http and https
 * @author xlongwei
 *
 */
public class Servers {

	public static void main(String[] args) {
		ServerConfig serverConfig = Server.getServerConfig();
		boolean enableHttps = serverConfig.isEnableHttps();
		boolean enableRegistry = serverConfig.isEnableRegistry();
		if(enableHttps==false && enableRegistry) {
			//http启动时禁用enableRegistry
			serverConfig.setEnableRegistry(false);
		}
		Server.main(args);
		if(enableHttps==false) {
			serverConfig.setEnableHttps(true);
			serverConfig.setEnableRegistry(enableRegistry);
			Server.main(args);
		}
	}

}
