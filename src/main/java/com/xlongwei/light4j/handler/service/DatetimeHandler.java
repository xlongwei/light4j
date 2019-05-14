package com.xlongwei.light4j.handler.service;

import java.util.Date;

import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.DateUtil;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.StringUtil;

import io.undertow.server.HttpServerExchange;

/**
 * datetime handler
 * @author xlongwei
 *
 */
public class DatetimeHandler extends AbstractHandler {

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		String datetime = DateUtil.format(new Date());
		HandlerUtil.setResp(exchange, StringUtil.params("datetime", datetime));
	}

}
