package com.xlongwei.light4j.handler.weixin;

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

	@Override
	public String handle(String content) {
		if(StringUtil.isBlank(content) || !content.startsWith(TAG) || StringUtil.isBlank(content=content.substring(TAG.length()))) {
			return null;
		}
		return PlateUtil.search(content);
	}

}
