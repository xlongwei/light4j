package com.xlongwei.light4j.handler.weixin;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xlongwei.light4j.util.JsonUtil;
import com.xlongwei.light4j.util.NumberUtil;
import com.xlongwei.light4j.util.RedisConfig;
import com.xlongwei.light4j.util.StringUtil;
import com.xlongwei.light4j.util.WeixinUtil.AbstractMessageHandler.AbstractTextHandler;

import cn.hutool.core.map.MapUtil;
import lombok.extern.slf4j.Slf4j;


/**
 * calc handler
 * @author xlongwei
 * 
 */
@Slf4j
public class CalcHandler extends AbstractTextHandler {
	private static final String TAG = "计算";
	private	Pattern pattern = Pattern.compile("[0-9a-z\\.\\,\\(\\+\\-\\*/%^\\)]{3,}");
	private Pattern params = Pattern.compile("([^ ]+)=(\\d+(\\.\\d+)?)");
	
	@Override
	public String handle(String content) {
		if(StringUtil.isBlank(content)) {
			return null;
		}
		if(pattern.matcher(content=StringUtil.toDBC(content)).matches() && !StringUtil.isNumbers(content)) {
			String parseExp = NumberUtil.parseExp(content);
			if(StringUtil.hasLength(parseExp)) {
				return parseExp;
			}
		}
		if(!content.startsWith(TAG)) {
			return null;
		}
		String exp = content.substring(2).trim();
		if(StringUtil.isBlank(exp)) {
			return null;
		}
		String key = "weixin.calc."+message.get().getFromUserName();
		if(exp.indexOf('=')>0) {
			Map<String, String> map = MapUtil.newHashMap();
			Matcher matcher = params.matcher(exp);
			while(matcher.find()) {
				map.put(matcher.group(1), matcher.group(2));
			}
			if(map.size() > 0) {
				RedisConfig.set(key, JSON.toJSONString(map));
				return map.toString();
			}else {
				return null;
			}
		}
		
		String formula = RedisConfig.get("weixin.key."+exp);
		if(StringUtil.hasLength(formula)) {
			log.info("{} = {}", exp, formula);
			exp = formula;
		}
		
		JSONObject json = JsonUtil.parse(RedisConfig.get(key));
		Map<String, Number> context = MapUtil.newHashMap();
		json.forEach((k, v) -> {
			String paramName = k, paramValue = v.toString();
			Number number = StringUtil.isNumbers(paramValue) ? NumberUtil.parseInt(paramValue, null) : null;
			if(number == null && StringUtil.isDecimal(paramValue)) {
				number = NumberUtil.parseDouble(paramValue, null);
			}
			if(number != null) {
				context.put(paramName, number);
			}
		});
		String parseExp = NumberUtil.parseExp(exp, context);
		if(StringUtil.hasLength(parseExp)) {
			if(StringUtil.hasLength(formula)) {
				return new StringBuilder(content.substring(2).trim()).append("：").append(context.toString()).append("\n").append(formula).append("=").append(parseExp).toString();
			}else {
				return parseExp;
			}
		}
		return "示例：sqrt(3^2)*sin(pi/2)";
	}

}
