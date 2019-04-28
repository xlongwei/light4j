package com.xlongwei.light4j.handler.service;

import java.util.Date;

import org.jose4j.json.internal.json_simple.JSONObject;

import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.DateUtil;

import io.undertow.server.HttpServerExchange;

public class DatetimeHandler extends AbstractHandler {

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		String datetime = DateUtil.format(new Date());
		exchange.putAttachment(AbstractHandler.RESP, JSONObject.toString("datetime", datetime));
	}

}
