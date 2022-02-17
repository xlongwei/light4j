package com.xlongwei.light4j.handler.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.DesUtil;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.StringUtil;

import de.rrze.jpwgen.utils.PwHelper;
import io.undertow.server.HttpServerExchange;

/**
 * des handler
 * @author xlongwei
 *
 */
public class DesHandler extends AbstractHandler {

	public void encrypt(HttpServerExchange exchange) throws Exception {
		String password = HandlerUtil.getParam(exchange, "password", "passwordCacheKey");
		String data = HandlerUtil.getParam(exchange, "data");
		String encrypt = DesUtil.getInstance(StringUtils.trimToEmpty(password)).doEncrypt(data==null ? "" : data);
		HandlerUtil.setResp(exchange, StringUtil.params("data", encrypt));
	}
	
	public void decrypt(HttpServerExchange exchange) throws Exception {
		String password = HandlerUtil.getParam(exchange, "password", "passwordCacheKey");
		String data = HandlerUtil.getParam(exchange, "data");
		String encrypt = DesUtil.getInstance(StringUtils.trimToEmpty(password)).doDecrypt(data==null ? "" : data);
		HandlerUtil.setResp(exchange, StringUtil.params("data", encrypt));
	}
	
	public void pwgen(HttpServerExchange exchange) throws Exception {
		String data = StringUtil.firstNotBlank(HandlerUtil.getParam(exchange, "options"), "-N 6 -s 24");
		List<String> passwords = PwHelper.process(StringUtils.split(data), null);
		if(passwords.size()>0) {
			Map<String, Object> map = new HashMap<>(1);
			map.put("data", passwords);
			HandlerUtil.setResp(exchange, map);
		}
	}
	
	public void pwcheck(HttpServerExchange exchange) throws Exception {
		String password = HandlerUtil.getParam(exchange, "password");
		HandlerUtil.setResp(exchange, DesUtil.pwcheck(password));
	}

}
