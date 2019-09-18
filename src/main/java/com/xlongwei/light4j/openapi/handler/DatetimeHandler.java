package com.xlongwei.light4j.openapi.handler;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.xnio.OptionMap;

import com.networknt.client.Http2Client;
import com.networknt.cluster.Cluster;
import com.networknt.server.Server;
import com.networknt.service.SingletonServiceFactory;
import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.JsonUtil;
import com.xlongwei.light4j.util.StringUtil;

import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import lombok.extern.slf4j.Slf4j;

/**
 * 用于体验consul registry功能，需要在server.yml开启enableRegistry，并配置client.trustore、consul.yml
 * @author xlongwei
 */
@Slf4j
public class DatetimeHandler extends AbstractHandler {
	static Cluster cluster = SingletonServiceFactory.getBean(Cluster.class);
    static String path = "/service/datetime";
    static String serviceId = Server.getServerConfig().getServiceId();
    static String tag = Server.getServerConfig().getEnvironment();
    static Http2Client client = Http2Client.getInstance();
    static Map<URI, ClientConnection> connections = new ConcurrentHashMap<>();

	@Override
	@SuppressWarnings({ "unchecked" })
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		List<URI> services = cluster.services("https", serviceId, tag);
		for(URI service : services) {
			try {
				ClientConnection connection = connections.get(service);
				if(connection == null || !connection.isOpen()) {
					connection = client.connect(service, Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
					connections.put(service, connection);
				}
				AtomicReference<ClientResponse> reference = new AtomicReference<>();
		        CountDownLatch latch = new CountDownLatch(1);
		        
		        ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath(path);
		        //缺少此行时报400错误
		        request.getRequestHeaders().put(Headers.HOST, "localhost");
		        //client.propagateHeaders(request, exchange);
	            connection.sendRequest(request, client.createClientCallback(reference, latch));
	            
	            latch.await();
            	String datetime = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
            	if(StringUtil.hasLength(datetime)) {
	            	HandlerUtil.setResp(exchange, JsonUtil.parse(datetime, Map.class));
	            	return;
            	}
			}catch(Exception e) {
				log.info("fail to request service: {}, ex: {}", service, e.getMessage());
			}
		}
	}

}
