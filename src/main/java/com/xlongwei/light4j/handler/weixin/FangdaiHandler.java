package com.xlongwei.light4j.handler.weixin;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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

import lombok.extern.slf4j.Slf4j;

/**
 * fangdai handler
 * @author xlongwei
 *
 */
@Slf4j
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
					String endYearMonth = DateUtil.format(DateUtils.addMonths(DateUtil.parse(ym+"01"), json.getIntValue(mKey)), "yyyyMMdd").substring(0, 6);
					if(n > json.getIntValue(mKey)) {
						return "已过还款截止年月："+endYearMonth+"，本金："+NumberUtil.format(json.getDoubleValue("A"),"#")+"，期数："+json.getIntValue("M")+"，月息："+NumberUtil.format(json.getDoubleValue("B"),".######");
					}
					Map<String, Number> ctx = new HashMap<>();
					ctx.put("A", json.getDouble("A"));
					ctx.put("M", json.getInteger("M"));
					ctx.put("B", json.getDouble("B"));
					String x = NumberUtil.format(ExpUtil.exp("(A-A/M*N)*B").context(ctx).context(mKey, json.getInteger(mKey)).context("N", n).parse().getResult(), ".##");
					String bj = NumberUtil.format(json.getDoubleValue("A") / json.getIntValue(mKey), ".##");
					String bx = NumberUtil.format(Double.parseDouble(x)+Double.parseDouble(bj), ".##");
					//本息总额=〔(总贷款额÷还款月数+总贷款额×月利率)+总贷款额÷还款月数×(1+月利率)〕÷2×还款月数
					String bxze = NumberUtil.format(ExpUtil.exp("((A/M+A*B)+A/M*(1+B))/2*M").context(ctx).parse().getResult(), ".##");
					double yhlx = 0;
					for(int i=1;i<n;i++) {
						yhlx += ExpUtil.exp("(A-A/M*N)*B").context(ctx).context("N", i).parse().getResult().doubleValue();
					}
					String yhbj = NumberUtil.format(n*json.getDoubleValue("A")/json.getIntValue(mKey), ".##");
					String dhbj = NumberUtil.format(json.getDouble("A") - n*json.getDoubleValue("A")/json.getIntValue(mKey), ".##");
					String dhlx = NumberUtil.format(Double.parseDouble(bxze) - json.getDouble("A") - yhlx, ".##");
					return new StringBuilder(ym2).append("(").append(n+1).append("/").append(json.getString(mKey)).append(")：本金").append(bj).append("，利息").append(x).append("，本息").append(bx)
							.append("\n\n本息总额：").append(bxze).append("，已还本金：").append(yhbj).append("，已还利息：").append(NumberUtil.format(yhlx, ".##"))
							.append("\n\n截至年月：").append(endYearMonth).append("，待还本金：").append(dhbj).append("，待还利息：").append(dhlx).toString();
				}
			}catch(Exception e) {
				log.warn("{} {}", e.getClass().getSimpleName(), e.getMessage());
			}
		}
		return RedisConfig.get("weixin.key.房贷");
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
