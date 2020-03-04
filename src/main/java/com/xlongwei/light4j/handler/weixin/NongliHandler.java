package com.xlongwei.light4j.handler.weixin;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.xlongwei.light4j.util.DateUtil;
import com.xlongwei.light4j.util.IdWorker.SystemClock;
import com.xlongwei.light4j.util.NumberUtil;
import com.xlongwei.light4j.util.StringUtil;
import com.xlongwei.light4j.util.WeixinUtil.AbstractMessageHandler.AbstractTextHandler;
import com.xlongwei.light4j.util.ZhDate;

public class NongliHandler extends AbstractTextHandler {
	
	private Pattern pattern = Pattern.compile("(\\d{4})[\\.\\-](\\d{1,2})[\\.\\-](\\d{1,2})([,，](true|false))?");

	@Override
	public String handle(String content) {
		if(StringUtil.isBlank(content)) {
			return null;
		}
		if(content.startsWith("农历")) {
			Date day = DateUtil.parseNow(content.substring(2));
			ZhDate zhDate = ZhDate.fromDate(day);
			int age = cn.hutool.core.date.DateUtil.ageOfNow(day);
			return zhDate.chinese()+" "+zhDate.ganzhi()+zhDate.shengxiao()+(age>0?" "+age:"");
		}else if(content.startsWith("阳历")) {
			Matcher matcher = pattern.matcher(content.substring(2));
			if(matcher.matches()) {
				int lunarYear = NumberUtil.parseInt(matcher.group(1), 2020), lunarMonth = NumberUtil.parseInt(matcher.group(2), 1), lunarDay = NumberUtil.parseInt(matcher.group(3), 1);
				boolean leapMonth = NumberUtil.parseBoolean(matcher.group(5), false);
				if(ZhDate.validate(lunarYear, lunarMonth, lunarDay, leapMonth)) {
					ZhDate zhDate = new ZhDate(lunarYear, lunarMonth, lunarDay, leapMonth);
					return DateUtil.dayFormat.format(zhDate.toDate());
				}else {
					return "农历日期不支持";
				}
			}else {
				return "示例：阳历2020.1.1";
			}
		}else if(content.startsWith("生肖") || content.startsWith("干支")) {
			int thisYear = Integer.valueOf(DateUtil.yearFormat.format(SystemClock.date()));
			int year = NumberUtil.parseInt(content.substring(2), thisYear);
			return year+ZhDate.ganzhi(year)+ZhDate.shengxiao(year)+(year>=thisYear?"":(thisYear-year));
		}
		return null;
	}

}
