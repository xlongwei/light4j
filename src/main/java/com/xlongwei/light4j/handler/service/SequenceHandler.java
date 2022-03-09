package com.xlongwei.light4j.handler.service;

import java.util.Collections;
import java.util.Date;

import com.networknt.utility.StringUtils;
import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.MySqlUtil;
import com.xlongwei.light4j.util.NumberUtil;
import com.xlongwei.light4j.util.RedisConfig;

import io.undertow.server.HttpServerExchange;

public class SequenceHandler extends AbstractHandler {

	public void next(HttpServerExchange exchange) throws Exception {
		String sequence = sequence(exchange);
		boolean mysql = "mysql".equals(HandlerUtil.getParam(exchange, "type"));
		long step = NumberUtil.parseLong(HandlerUtil.getParam(exchange, "step"), 0L);
		if(step > 1) {
			step -= 1; //这个很奇怪
			long next = mysql ? MySqlUtil.Sequence.next(sequence, step) : RedisConfig.Sequence.next(sequence, step);
			HandlerUtil.setResp(exchange, Collections.singletonMap("next", next));
		}
		long next = mysql ? MySqlUtil.Sequence.next(sequence) : RedisConfig.Sequence.next(sequence);
		String format = HandlerUtil.getParam(exchange, "format");
		if (StringUtils.isNotBlank(format)) {
			try {
				HandlerUtil.setResp(exchange,
						Collections.singletonMap("next", String.format(format, next, new Date())));
				return;
			} catch (Exception e) {
				// ignore
			}
		}
		HandlerUtil.setResp(exchange, Collections.singletonMap("next", next));
	}
	
	public void update(HttpServerExchange exchange) throws Exception {
		String sequence = sequence(exchange);
		boolean mysql = "mysql".equals(HandlerUtil.getParam(exchange, "type"));
		long value = NumberUtil.parseLong(HandlerUtil.getParam(exchange, "value"), 0L);
		boolean update = mysql ? MySqlUtil.Sequence.update(sequence, value) : RedisConfig.Sequence.update(sequence, value);
		HandlerUtil.setResp(exchange, Collections.singletonMap("update", update));
	}

	private String sequence(HttpServerExchange exchange) {
		String name = StringUtils.trimToEmpty(HandlerUtil.getParam(exchange, "name"));
		String userName = "common";
		if(StringUtils.isNotBlank(HandlerUtil.getShowapiUserName(exchange))) {
			String showapiUserName = HandlerUtil.getShowapiUserName(exchange);
			userName = StringUtils.isNotBlank(showapiUserName) ? showapiUserName : userName;
		}
		return userName + "." + name;
	}
}
