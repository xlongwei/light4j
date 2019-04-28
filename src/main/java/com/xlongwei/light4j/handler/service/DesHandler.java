package com.xlongwei.light4j.handler.service;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jose4j.json.internal.json_simple.JSONObject;

import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.DesUtil;

import de.rrze.jpwgen.utils.PwHelper;
import io.undertow.server.HttpServerExchange;

public class DesHandler extends AbstractHandler {

	public void encrypt(HttpServerExchange exchange) throws Exception {
		String password = getParam(exchange, "password");
		String data = getParam(exchange, "data");
		String encrypt = DesUtil.getInstance(StringUtils.trimToEmpty(password)).doEncrypt(data);
		exchange.putAttachment(AbstractHandler.RESP, JSONObject.toString("data", encrypt));
	}
	
	public void decrypt(HttpServerExchange exchange) throws Exception {
		String password = getParam(exchange, "password");
		String data = getParam(exchange, "data");
		String encrypt = DesUtil.getInstance(StringUtils.trimToEmpty(password)).doDecrypt(data);
		exchange.putAttachment(AbstractHandler.RESP, JSONObject.toString("data", encrypt));
	}
	
	public void pwgen(HttpServerExchange exchange) throws Exception {
		String data = StringUtils.defaultString(getParam(exchange, "options"), "-N 6 -s 24");
		List<String> passwords = PwHelper.process(StringUtils.split(data), null);
		if(passwords.size()>0) {
			exchange.putAttachment(AbstractHandler.RESP, JSONObject.toString("data", passwords));
		}
	}

}
