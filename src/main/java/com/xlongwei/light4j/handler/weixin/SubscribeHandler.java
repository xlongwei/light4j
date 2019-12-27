package com.xlongwei.light4j.handler.weixin;

import com.xlongwei.light4j.util.RedisConfig;
import com.xlongwei.light4j.util.StringUtil;
import com.xlongwei.light4j.util.WeixinUtil;
import com.xlongwei.light4j.util.WeixinUtil.AbstractEvent.SubscribeEvent;
import com.xlongwei.light4j.util.WeixinUtil.AbstractEventHandler;
import com.xlongwei.light4j.util.WeixinUtil.AbstractMessage;
import com.xlongwei.light4j.util.WeixinUtil.AbstractMessage.TextMessage;

import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * subscribe handler
 * @author xlongwei
 *
 */
@Slf4j
public class SubscribeHandler extends AbstractEventHandler<SubscribeEvent> {

	public static final String WEIXIN_SUBSCRIBE = "weixin.subscribe.";

	@Override
	public AbstractMessage handle(SubscribeEvent msg) {
		TextMessage textMessage = new TextMessage();
		String string = null;
		String fromUserName = msg.getFromUserName();
		String toUserName = msg.getToUserName();
		boolean isTest = WeixinUtil.touserTest.equals(toUserName);
		log.info("weixin.subscribe from={} to={} test={}", fromUserName, toUserName, isTest);
		if(isTest) {
			RedisConfig.persist(RedisConfig.CACHE, WEIXIN_SUBSCRIBE+fromUserName, DateUtil.now());
			string = "欢迎关注，您的openid是：\n" + fromUserName;
		}else {
			string = RedisConfig.get("weixin.key.help");
		}
		textMessage.setContent(StringUtil.firstNotBlank(string, "欢迎关注！"));
		return textMessage;
	}

}
