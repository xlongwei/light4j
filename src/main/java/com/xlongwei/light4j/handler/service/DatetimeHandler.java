package com.xlongwei.light4j.handler.service;

import org.apache.commons.lang3.time.FastDateFormat;

import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.IdWorker.SystemClock;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

/**
 * datetime handler
 * @author xlongwei
 *
 */
public class DatetimeHandler extends AbstractHandler {
	private static FastDateFormat fastDateFormat = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
        exchange.setStatusCode(200);
        String datetime = fastDateFormat.format(SystemClock.now());
		exchange.getResponseSender().send("{\"datetime\":\""+datetime+"\"}");
	}

}
