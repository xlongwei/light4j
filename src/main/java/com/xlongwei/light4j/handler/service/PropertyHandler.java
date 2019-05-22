package com.xlongwei.light4j.handler.service;

import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.NumberUtil;
import com.xlongwei.light4j.util.RedisConfig;
import com.xlongwei.light4j.util.StringUtil;

import io.undertow.server.HttpServerExchange;

/**
 * property handler
 * @author xlongwei
 *
 */
public class PropertyHandler extends AbstractHandler {

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		String key = HandlerUtil.getParam(exchange, "key");
		if(StringUtil.isBlank(key)) {
			return;
		}
		String value = HandlerUtil.getParam(exchange, "value");
		String type = HandlerUtil.getParam(exchange, "type");
		int expires = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "expire"), 0);
		if(StringUtil.isBlank(type)) {
			type = StringUtil.isBlank(value)==false||expires>0 ? "set" : "get";
		}
		String set = "set";
		if(set.equals(type)) {
			if(expires == 0) {
				RedisConfig.set(key, value);
			}else if(expires > 0){
				RedisConfig.set(RedisConfig.CACHE, key, value, expires);
			}else {
				RedisConfig.persist(key, value);
			}
			HandlerUtil.setResp(exchange, StringUtil.params("value", value));
		}else {
			String get = RedisConfig.get(key);
			HandlerUtil.setResp(exchange, StringUtil.params("value", get));
		}
	}

}
