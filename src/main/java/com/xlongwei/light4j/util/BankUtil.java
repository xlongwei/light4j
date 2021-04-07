package com.xlongwei.light4j.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import lombok.Data;

/**
 * 银行卡号信息查询
 * @author xlongwei
 *
 */
public class BankUtil {
	static CardBin cardBin = new CardBin();
	static Map<String, CardInfo> cardMap = new HashMap<>();
	
	/**
	 * 通过银行卡号查找卡片信息
	 * @param accountNo
	 * @return CardInfo
	 */
	public static CardInfo cardInfo(String accountNo) {
		String bin = cardBin.bin(accountNo);
		return bin==null ? null : cardMap.get(bin);
	}
	
	/** 通过银行卡号查找卡bin码 */
	public static String cardBin(String accountNo) {
		return cardBin.bin(accountNo);
	}
	
	/**
	 * 添加基础卡片信息
	 * @param cardInfo
	 */
	public static void addData(CardInfo cardInfo) {
		cardBin.add(cardInfo.getCardBin());
		cardMap.put(cardInfo.getCardBin(), cardInfo);
	}
	
	/** 添加卡bin码 */
	public static void addBin(String bin) {
		cardBin.add(bin);
	}

	@Data
	public static class CardInfo {
		private String cardBin;//620058
		private String bankId;//01020000
		private String bankName;//中国工商银行
		private String cardName;//银联标准卡
		private String cardDigits;//19 卡号长度
		private String cardType;//借记卡
		private String bankCode;//ICBC
		private String bankName2;//中国工商银行
		public String rowOut() {
			return StringUtil.join(Arrays.asList(cardBin,bankId,bankName,cardName,cardDigits,cardType,bankCode,bankName2), null, null, ",");
		}
		public CardInfo rowIn(String row) {
			//需要指定limit>=8，增加字段时需要注意limit值>=字段数
			String[] split = row.split("[,]", 18);
			int idx = 0;
			cardBin = split[idx++];
			bankId = split[idx++];
			bankName = split[idx++];
			cardName = split[idx++];
			cardDigits = NumberUtil.correctNumber(split[idx++]);
			cardType = split[idx++];
			bankCode = split[idx++];
			bankName2 = split[idx++];
			return this;
		}		
	}
}
