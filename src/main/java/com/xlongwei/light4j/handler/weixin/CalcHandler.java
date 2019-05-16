package com.xlongwei.light4j.handler.weixin;

import java.util.regex.Pattern;

import com.xlongwei.light4j.util.NumberUtil;
import com.xlongwei.light4j.util.StringUtil;
import com.xlongwei.light4j.util.WeixinUtil.AbstractMessageHandler.AbstractTextHandler;


/**
 * calc handler
 * @author xlongwei
 * 
 */
public class CalcHandler extends AbstractTextHandler {
	private static final String TAG = "计算";
	private	Pattern pattern = Pattern.compile("[0-9a-z\\.\\,\\(\\+\\-\\*/%^\\)]{3,}");
	
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
		String exp = content.substring(2);
		String parseExp = NumberUtil.parseExp(exp);
		if(StringUtil.hasLength(parseExp)) {
			return parseExp;
		}
		return "示例：sqrt(3^2)*sin(pi/2)";
	}

}
