package com.xlongwei.light4j.handler.service;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.BankUtil;
import com.xlongwei.light4j.util.BankUtil.CardInfo;
import com.xlongwei.light4j.util.ConfigUtil;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.StringUtil;

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
		if(StringUtil.isNumbers(bankCardNumber)) {
			CardInfo cardInfo = BankUtil.cardInfo(bankCardNumber);
			Map<String, String> map = new HashMap<>(16);
			map.put("valid", Boolean.toString(StringUtil.isBankCardNumber(bankCardNumber)));
			if(cardInfo != null) {
				map.put("cardBin", cardInfo.getCardBin());
				map.put("bankId", cardInfo.getBankId());
				map.put("bankName", cardInfo.getBankName());
				map.put("cardName", cardInfo.getCardName());
				map.put("cardDigits", cardInfo.getCardDigits());
				map.put("cardType", cardInfo.getCardType());
				map.put("bankCode", cardInfo.getBankCode());
				map.put("bankName2", cardInfo.getBankName2());
			}
			HandlerUtil.setResp(exchange, map);
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
