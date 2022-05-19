package com.xlongwei.light4j.handler.service;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.networknt.utility.StringUtils;
import com.xlongwei.light4j.apijson.DemoApplication;
import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.BankUtil;
import com.xlongwei.light4j.util.BankUtil.CardInfo;
import com.xlongwei.light4j.util.ConfigUtil;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.MySqlUtil;
import com.xlongwei.light4j.util.StringUtil;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.beetl.sql.core.SQLReady;

import io.undertow.server.HttpServerExchange;
import lombok.extern.slf4j.Slf4j;

/**
 * bank card handler
 * @author xlongwei
 *
 */
@Slf4j
public class BankCardHandler extends AbstractHandler {
	static boolean loadFromFile = !DemoApplication.apijsonEnabled;//停用apijson时不从数据库加载数据
	static Cache<String, CardInfo> cache = null;
	static boolean cardBin = BooleanUtils.toBoolean(System.getenv("bank.cardBin"));
	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		String bankCardNumber = HandlerUtil.getParam(exchange, "bankCardNumber");
		if(StringUtil.isNumbers(bankCardNumber)) {
			CardInfo cardInfo = cardInfo(bankCardNumber);
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
	
	public static CardInfo cardInfo(String bankCardNumber) {
		if(!StringUtil.isNumbers(bankCardNumber)) {
			return null;
		}else if(loadFromFile) {
			return BankUtil.cardInfo(bankCardNumber);
		}else if(cardBin) {
			String cardBin = BankUtil.cardBin(bankCardNumber);
			return StringUtils.isBlank(cardBin) ? null : cache.get(cardBin, (bin) -> MySqlUtil.SQLMANAGER.executeQueryOne(new SQLReady("select cardBin,issuerCode as bankId,issuerName as bankName,cardName,cardDigits,cardType,bankCode,bankName as bankName2 from bank_card where cardBin=?", bin), CardInfo.class));
		}else {
			return MySqlUtil.SQLMANAGER.executeQueryOne(new SQLReady("select cardBin,issuerCode as bankId,issuerName as bankName,cardName,cardDigits,cardType,bankCode,bankName as bankName2 from bank_card where cardBin=?", bankCardNumber), CardInfo.class);
		}
	}

	static {
		if(loadFromFile == false) {
			if(cardBin){
				MySqlUtil.SQLMANAGER.execute(new SQLReady("select cardBin from bank_card"), CardInfo.class).forEach(cardInfo -> {
					BankUtil.addBin(cardInfo.getCardBin());
				});
				cache =  Caffeine.newBuilder()
					.expireAfterAccess(5, TimeUnit.MINUTES)
					.maximumSize(256)
					.build();
			}
			log.info("bank_card loaded cardBin={}", cardBin);
		}else {
			try(InputStream inputStream = new BufferedInputStream(ConfigUtil.stream("cardBin.txt"))) {
				IOUtils.lineIterator(inputStream, StandardCharsets.UTF_8).forEachRemaining(line -> BankUtil.addData(new CardInfo().rowIn(line)));
				log.info("cardBin.txt loaded");
			}catch (Exception e) {
				log.warn("fail to init cardBin.txt, ex: {} {}", e.getClass().getSimpleName(), e.getMessage());
			}
		}
	}
}
