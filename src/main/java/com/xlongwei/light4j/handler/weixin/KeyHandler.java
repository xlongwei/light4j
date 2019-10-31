package com.xlongwei.light4j.handler.weixin;

import java.util.Map;

import com.xlongwei.light4j.handler.service.IpHandler;
import com.xlongwei.light4j.handler.service.MobileHandler;
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
			if(StringUtil.isIp(content)) {
				Map<String, String> searchToMap = IpHandler.searchToMap(content);
				if(searchToMap!=null) {
					return searchToMap.get("region");
				}
			}else if(StringUtil.getMobileType(content)>0) {
				Map<String, String> searchToMap = MobileHandler.searchToMap(content);
				if(searchToMap!=null) {
					return StringUtil.firstNotBlank(searchToMap.get("region"), MobileHandler.NUMBER_TYPE[StringUtil.getMobileType(content)]);
				}
			}else {
				String string = RedisConfig.get("weixin.key."+content);
				if(!StringUtil.isBlank(string)) {
					return string;
				}
			}
		}
		return null;
	}

}
