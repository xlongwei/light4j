package com.xlongwei.light4j.openapi.handler;

import com.networknt.cors.CorsUtil;
import com.networknt.utility.StringUtils;
import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.StringUtil;

import cn.hutool.core.map.MapUtil;
import io.undertow.server.HttpServerExchange;

/**
 * service handler
 * @author xlongwei
 *
 */
public class ServiceHandler extends AbstractHandler {

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		String service = exchange.getRequestHeaders().getFirst(com.xlongwei.light4j.handler.ServiceHandler.HANDLER_PATH);
		String name = HandlerUtil.getParam(exchange, "handler"), path = HandlerUtil.getParam(exchange, "path");
		if (StringUtil.hasLength(service)) {
			String[] split = StringUtils.split(service, "/");
			name = split[0];
			path = split.length > 1 ? split[1] : null;
		}
		if(StringUtils.isBlank(name)) {
			return;
		}
		AbstractHandler handler = com.xlongwei.light4j.handler.ServiceHandler.handlers.get(name);
		if(handler==null) {
			return;
		}
		if(CorsUtil.isPreflightedRequest(exchange)) {
			HandlerUtil.setResp(exchange, MapUtil.newHashMap());
		}else {
			exchange.putAttachment(AbstractHandler.PATH, StringUtils.isBlank(path)?"":path);
			handler.handleRequest(exchange);
			com.xlongwei.light4j.handler.ServiceHandler.serviceCount(name);
		}
	}

}
