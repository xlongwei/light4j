package com.xlongwei.light4j.openapi.handler;

import org.nlpcn.commons.lang.util.StringUtil;

import com.networknt.cors.CorsUtil;
import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.HandlerUtil;

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
		String name = HandlerUtil.getParam(exchange, "handler");
		if(StringUtil.isBlank(name)) {
			return;
		}
		AbstractHandler handler = com.xlongwei.light4j.handler.ServiceHandler.handlers.get(name);
		if(handler==null) {
			return;
		}
		if(CorsUtil.isPreflightedRequest(exchange)) {
			HandlerUtil.setResp(exchange, MapUtil.newHashMap());
		}else {
			String path = HandlerUtil.getParam(exchange, "path");
			exchange.putAttachment(AbstractHandler.PATH, StringUtil.isBlank(path)?"":path);
			handler.handleRequest(exchange);
			com.xlongwei.light4j.handler.ServiceHandler.serviceCount(name);
		}
	}

}
