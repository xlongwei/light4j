package com.xlongwei.light4j.openapi.extend;

import com.networknt.cors.CorsUtil;
import com.networknt.handler.Handler;
import com.networknt.openapi.JwtVerifyHandler;

import io.undertow.server.HttpServerExchange;

/**
 * 跳过浏览器偶尔发出的OPTIONS请求，响应{}
 * @author xlongwei
 *
 */
public class MyJwtVerifyHandler extends JwtVerifyHandler {

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		if(CorsUtil.isPreflightedRequest(exchange)) {
			Handler.next(exchange, getNext());
		}else {
			super.handleRequest(exchange);
		}
	}

}
