package com.xlongwei.light4j;

import com.networknt.server.Server;

/**
 * listen both http and https
 * @author xlongwei
 *
 */
public class Servers {

	public static void main(String[] args) {
		Server.main(args);
		if(Server.config.isEnableHttps()==false) {
			Server.config.setEnableHttps(true);
			Server.main(args);
		}
	}

}
