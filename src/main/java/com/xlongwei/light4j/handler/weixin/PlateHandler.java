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
	
	@Override
	public String handle(String content) {
		if(StringUtil.isBlank(content)) {
			return null;
		}
		boolean search = false;
		if(content.length()>3 && content.startsWith("车牌")) {
			content = content.charAt(2)=='号' ? content.substring(3) : content.substring(2);
			search = true;
		}else {
			search = StringUtil.isPlateNumber(content);
		}
		return search ? PlateUtil.search(content) : null;
	}

}
