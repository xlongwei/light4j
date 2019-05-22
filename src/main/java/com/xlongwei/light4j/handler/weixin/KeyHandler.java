package com.xlongwei.light4j.handler.weixin;

import com.xlongwei.light4j.util.RedisConfig;
import com.xlongwei.light4j.util.StringUtil;
import com.xlongwei.light4j.util.WeixinUtil.AbstractMessageHandler.AbstractTextHandler;

/**
 * key handler
 * @author xlongwei
 *
 */
public class KeyHandler extends AbstractTextHandler {
	
	@Override
	public String handle(String content) {
		if(!StringUtil.isBlank(content)) {
			String string = RedisConfig.get("weixin.key."+content);
			if(!StringUtil.isBlank(string)) {
				return string;
			}
		}
		return null;
	}

}
