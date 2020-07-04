package com.xlongwei.light4j.handler.service;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.jsoup.helper.StringUtil;

import com.alibaba.fastjson.JSON;
import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.BankUtil;
import com.xlongwei.light4j.util.BankUtil.CardInfo;
import com.xlongwei.light4j.util.ConfigUtil;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.JsonUtil;

import io.undertow.server.HttpServerExchange;
import lombok.extern.slf4j.Slf4j;

/**
 * bank card handler
 * @author xlongwei
 *
 */
@Slf4j
public class BankCardHandler extends AbstractHandler {

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		String bankCardNumber = HandlerUtil.getParam(exchange, "bankCardNumber");
		if(StringUtil.isNumeric(bankCardNumber)) {
			CardInfo cardInfo = BankUtil.cardInfo(bankCardNumber);
			if(cardInfo != null) {
				HandlerUtil.setResp(exchange, JsonUtil.parse(JSON.toJSONString(cardInfo)));
			}
		}
	}

	static {
		try(InputStream inputStream = new BufferedInputStream(ConfigUtil.stream("cardBin.txt"))) {
			IOUtils.lineIterator(inputStream, StandardCharsets.UTF_8).forEachRemaining(line -> BankUtil.addData(new CardInfo().rowIn(line)));
			log.info("cardBin.txt loaded");
		}catch (Exception e) {
			log.warn("fail to init cardBin.txt, ex: {} {}", e.getClass().getSimpleName(), e.getMessage());
		}
	}
}
