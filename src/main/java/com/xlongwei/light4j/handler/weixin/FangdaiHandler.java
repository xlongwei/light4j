package com.xlongwei.light4j.handler.weixin;

import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xlongwei.light4j.util.DateUtil;
import com.xlongwei.light4j.util.ExpUtil;
import com.xlongwei.light4j.util.JsonUtil;
import com.xlongwei.light4j.util.NumberUtil;
import com.xlongwei.light4j.util.RedisConfig;
import com.xlongwei.light4j.util.StringUtil;
import com.xlongwei.light4j.util.WeixinUtil.AbstractMessageHandler.AbstractTextHandler;

/**
 * fangdai handler
 * @author xlongwei
 *
 */
public class FangdaiHandler extends AbstractTextHandler {
	private static final String TAG = "房贷";
	
	@Override
	public String handle(String content) {
		if(StringUtil.isBlank(content) || !content.startsWith(TAG)) {
			return null;
		}
		String[] arr = content.substring(TAG.length()).trim().split("[ \t,;，；]");
		String key = "weixin.fangdai."+message.get().getFromUserName();
		int paramsCount = 4;
		String mKey = "M";
		if(arr.length==paramsCount) {
			//房贷 本金 利息 期数 首次还款年月
			double a = NumberUtil.parseDouble(arr[0], 500000.0);
			double b = NumberUtil.parseDouble(arr[1], 0.0325) / 12;
			int m = NumberUtil.parseInt(NumberUtil.parseExp(arr[2]), 22);
			int guessYearMax = 31;
			if(m < guessYearMax) {
				m = m * 12;
			}
			String ym = StringUtil.firstNotBlank(yearMonth(arr[3]), "201902");
			JSON json = JsonUtil.builder(false).put("A", a).put("B", b).put(mKey, m).put("YM", ym).build();
			RedisConfig.persist(key, json.toJSONString());
			return new StringBuilder("本金：").append(a).append("，月息：").append(NumberUtil.format(b, ".######")).append("，期数：").append(m).append("，首次还款年月：").append(ym).toString();
		}else if(arr.length<=1) {
			String jsonStr = RedisConfig.get(key);
			if(StringUtil.isBlank(jsonStr)) {
				return null;
			}
			String ym2 = DateUtil.format(new Date(), "yyyyMMdd").substring(0, 6);
			if(arr.length==1) {
				ym2 = StringUtil.firstNotBlank(yearMonth(arr[0]), ym2);
			}
			JSONObject json = JsonUtil.parse(jsonStr);
			try {
				String ym = json.getString("YM");
				int y2 = NumberUtil.parseInt(ym2.substring(0, paramsCount), 2019), y = NumberUtil.parseInt(ym.substring(0, paramsCount), 2019);
				int m2 = NumberUtil.parseInt(ym2.substring(paramsCount, 6), 9), m = NumberUtil.parseInt(ym.substring(paramsCount, 6), 2);
				boolean after = y2>y || (y2==y && m2>=m);
				if(after) {
					int n = 12*(y2 - y) + (m2 - m);
					if(n > json.getIntValue(mKey)) {
						return "已过还款截止年月："+DateUtil.format(DateUtils.addMonths(DateUtil.parse(ym+"01"), json.getIntValue(mKey)), "yyyyMMdd").substring(0, 6);
					}
					String x = NumberUtil.format(ExpUtil.exp("(A-A/M*N)*B")
							.context("A", json.getDouble("A"))
							.context(mKey, json.getInteger(mKey))
							.context("N", n)
							.context("B", json.getDouble("B"))
							.parse().getResult(), ".##");
					String bj = NumberUtil.format(json.getDoubleValue("A") / json.getIntValue(mKey), ".##");
					String bx = NumberUtil.format(Double.parseDouble(x)+Double.parseDouble(bj), ".##");
					return new StringBuilder(ym2).append("(").append(n+1).append("/").append(json.getString(mKey)).append(")：本金").append(bj).append("，利息").append(x).append("，本息").append(bx).toString();
				}
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	private String yearMonth(String str) {
		if(StringUtil.hasLength(str) && StringUtil.isNumbers(str)) {
			switch(str.length()) {
			case 4: return "20"+str;
			case 6: return str;
			case 8: return str.substring(0, 6);
			default: return null;
			}
		}
		return null;
	}
}
