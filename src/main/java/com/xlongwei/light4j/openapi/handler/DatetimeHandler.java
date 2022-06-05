package com.xlongwei.light4j.openapi.handler;

import java.net.URI;
import java.util.List;
import java.util.Map;

import com.networknt.cluster.Cluster;
import com.networknt.server.Server;
import com.networknt.server.ServerConfig;
import com.networknt.server.Servers;
import com.networknt.service.SingletonServiceFactory;
import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.Http2Util;
import com.xlongwei.light4j.util.JsonUtil;
import com.xlongwei.light4j.util.StringUtil;

import io.undertow.client.ClientRequest;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Methods;

/**
 * 用于体验consul registry功能，需要在server.yml开启enableRegistry，并配置client.trustore、consul.yml
 * @author xlongwei
 */
public class DatetimeHandler extends AbstractHandler {
	static Cluster cluster = SingletonServiceFactory.getBean(Cluster.class);
	static ServerConfig serverConfig = Servers.currentPort!=-1 ? Servers.getServerConfig() : Server.getServerConfig();
	static String protocal = serverConfig.isEnableHttps() ? "https" : "http";
	static URI localhost = null;
    static String path = "/service/datetime";
    static String serviceId = Server.getServerConfig().getServiceId();
    static String tag = Server.getServerConfig().getEnvironment();

	@Override
	@SuppressWarnings({ "unchecked" })
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		if (Server.getServerConfig().isEnableRegistry() == false) {
			// enableRegistry=false时可以没有consul，此时可避免自动重试连接consul
			if (localhost == null) {
				// localhost与启动类有关，light4j的start.sh使用了自定义的Servers会同时监听两个端口，也可以使用启动类为Server
				localhost = new URI(protocal + "://localhost:" + (Servers.currentPort!=-1 ? Servers.currentPort : Server.currentPort));
			}
			String datetime = Http2Util.execute(localhost, new ClientRequest().setMethod(Methods.GET).setPath(path), null);
			HandlerUtil.setResp(exchange, JsonUtil.parse(datetime, Map.class));
			return;
		}
		List<URI> services = cluster.services(protocal, serviceId, tag);
		for (URI service : services) {
			// 通过循环可以自动重试下一个节点
			String datetime = Http2Util.execute(service, new ClientRequest().setMethod(Methods.GET).setPath(path), null);
			if (StringUtil.hasLength(datetime)) {
				HandlerUtil.setResp(exchange, JsonUtil.parse(datetime, Map.class));
				return;
			}
		}
	}

}
