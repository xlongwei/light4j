package com.xlongwei.light4j.handler.weixin;

import java.util.regex.Pattern;

import com.xlongwei.light4j.util.PlateUtil;
import com.xlongwei.light4j.util.StringUtil;
import com.xlongwei.light4j.util.WeixinUtil.AbstractMessageHandler.AbstractTextHandler;

/**
 * plate handler
 * @author xlongwei
 *
 */
public class PlateHandler extends AbstractTextHandler {
	
	private static final String TAG = "车牌";
	private static Pattern pattern = Pattern.compile("[京沪津渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤川青藏琼宁][a-zA-Z]-?[0-9a-zA-Z]{0,5}");

	@Override
	public String handle(String content) {
		if(StringUtil.isBlank(content) || (content.startsWith(TAG) && StringUtil.isBlank(content=content.substring(TAG.length()))) || pattern.matcher(content).matches()==false) {
			return null;
		}
		return PlateUtil.search(content);
	}

}
