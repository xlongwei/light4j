package com.xlongwei.light4j.handler.weixin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import com.xlongwei.light4j.handler.service.IpHandler;
import com.xlongwei.light4j.handler.service.MobileHandler;
import com.xlongwei.light4j.util.JsonUtil;
import com.xlongwei.light4j.util.RedisConfig;
import com.xlongwei.light4j.util.StringUtil;
import com.xlongwei.light4j.util.WeixinUtil.AbstractMessageHandler.AbstractTextHandler;

import cn.hutool.core.map.MapUtil;
import cn.hutool.http.HttpUtil;

/**
 * key handler
 * @author xlongwei
 *
 */
public class KeyHandler extends AbstractTextHandler {
	
	@Override
	public String handle(String content) {
		if(!StringUtil.isBlank(content)) {
			if("openid".equals(content)) {
				return message.get().getFromUserName();
			}else if("metric".equals(content)){
				String string = HttpUtil.get("https://log.xlongwei.com/log?type=metric");
				@SuppressWarnings("unchecked")
				Map<String, ?> map = JsonUtil.parse(string, Map.class);
				if(MapUtil.isNotEmpty(map)) {
					StringBuilder response = new StringBuilder();
					ArrayList<String> list = new ArrayList<>(map.keySet());
					Collections.sort(list);
					for(String key : list) {
						Object value = map.get(key);
						response.append(key).append('=').append(value).append('\n');
					}
					return response.toString();
				}
			}else if(StringUtil.isIp(content)) {
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
