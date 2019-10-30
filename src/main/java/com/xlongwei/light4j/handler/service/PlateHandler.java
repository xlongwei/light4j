package com.xlongwei.light4j.handler.service;

import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.PlateUtil;

import cn.hutool.core.map.MapUtil;
import io.undertow.server.HttpServerExchange;

/**
 * plate handler
 * @author xlongwei
 *
 */
public class PlateHandler extends AbstractHandler {

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		String text = HandlerUtil.getParam(exchange, "text");
		text = PlateUtil.search(text);
		if(text != null) {
			HandlerUtil.setResp(exchange, MapUtil.of("text", text));
		}
	}

}
