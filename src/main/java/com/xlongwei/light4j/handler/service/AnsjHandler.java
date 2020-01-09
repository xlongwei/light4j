package com.xlongwei.light4j.handler.service;

import java.util.List;
import java.util.ListIterator;

import com.networknt.utility.StringUtils;
import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.FenciUtil;
import com.xlongwei.light4j.util.FenciUtil.Method;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.NumberUtil;
import com.xlongwei.light4j.util.StringUtil;

import io.undertow.server.HttpServerExchange;

/**
 * ansj handler
 * @author xlongwei
 *
 */
public class AnsjHandler extends AbstractHandler {

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		String path = exchange.getAttachment(AbstractHandler.PATH);
		if(StringUtils.isBlank(path)) {
			return;
		}
		String text = HandlerUtil.getParam(exchange, "text");
		if(StringUtils.isBlank(text)) {
			return;
		}
		List<String> list = null; String string = null;
		switch(path) {
		case "fenci":
			list = FenciUtil.fenci(text, NumberUtil.parseEnum(HandlerUtil.getParam(exchange, "method"), Method.TO));
			break;
		case "keywords":
			int num = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "num"), 10);
			list = FenciUtil.keywords(HandlerUtil.getParam(exchange, "title"), text, num, NumberUtil.parseEnum(HandlerUtil.getParam(exchange, "method"), Method.TO));
			break;
		case "summary":
			boolean red = NumberUtil.parseBoolean(HandlerUtil.getParam(exchange, "red"), true);
			string = FenciUtil.summary(HandlerUtil.getParam(exchange, "title"), text, red, NumberUtil.parseEnum(HandlerUtil.getParam(exchange, "method"), Method.TO));
			break;
		case "jianfan":
			boolean fan = NumberUtil.parseBoolean(HandlerUtil.getParam(exchange, "fan"), true);
			string = FenciUtil.jianfan(text, fan);
			break;
		case "pinyin":
			int caseType = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "caseType"), 0);
			int toneType = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "toneType"), 0);
			int vcharType = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "vcharType"), 0);
			list = FenciUtil.pinyin(text, caseType, toneType, vcharType);
			ListIterator<String> listIterator = list.listIterator();
			while(listIterator.hasNext()) {
				if(listIterator.next()==null) {
					listIterator.set("　");
				}
			}
			break;
		default:
			break;
		}
		if(list!=null) {
			HandlerUtil.setResp(exchange, StringUtil.params("result", StringUtil.join(list, null, null, " ")));
		}else if(string!=null) {
			HandlerUtil.setResp(exchange, StringUtil.params("result", string));
		}
	}

}
