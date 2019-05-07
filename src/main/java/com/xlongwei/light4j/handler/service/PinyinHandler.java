package com.xlongwei.light4j.handler.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.jose4j.json.internal.json_simple.JSONObject;

import com.networknt.utility.StringUtils;
import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.NumberUtil;
import com.xlongwei.light4j.util.PinyinUtil;
import com.xlongwei.light4j.util.StringUtil;

import io.undertow.server.HttpServerExchange;

public class PinyinHandler extends AbstractHandler {

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		String text = HandlerUtil.getParam(exchange, "text");
		if(StringUtils.isNotBlank(text)) {
			int caseType = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "caseType"), 0);
			int toneType = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "toneType"), 0);
			int vcharType = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "vcharType"), 0);
			
			boolean isWord = text.length()==1;
			String[] pinyin = isWord ? PinyinUtil.getPinyin(text.charAt(0), caseType, toneType, vcharType) : PinyinUtil.getPinyin(text, caseType, toneType, vcharType);
			String join = StringUtil.join(Arrays.asList(pinyin), null, null, " ");
			Map<String, String> map = new HashMap<>();
			map.put("pinyin", join);
			if(!isWord) {
				StringBuilder header = new StringBuilder();
				for(String py : pinyin) header.append(py.charAt(0));
				map.put("header", header.toString());
			}
			exchange.putAttachment(AbstractHandler.RESP, JSONObject.toJSONString(map));
		}
	}

}
