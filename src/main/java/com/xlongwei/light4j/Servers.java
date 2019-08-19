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
		Server.main(args);
		ServerConfig serverConfig = Server.getServerConfig();
		if(serverConfig.isEnableHttps()==false) {
			serverConfig.setEnableHttps(true);
			Server.main(args);
		}
	}

}
