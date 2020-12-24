package com.xlongwei.light4j.provider;

import java.util.List;

import com.xlongwei.light4j.util.DateUtil;
import com.xlongwei.light4j.util.IdWorker.SystemClock;
import com.xlongwei.light4j.util.NumberUtil;
import com.xlongwei.light4j.util.RedisConfig;

import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import lombok.extern.slf4j.Slf4j;

/**
 * 简单群聊
 * @author xlongwei
 *
 */
@Slf4j
public class WebSocketChatCallback extends WebSocketAbstractCallback {
	private final String key = "ws.chat";
	private final int length = 18;
	boolean mute = false;

	@Override
	protected void onTextMessage(WebSocketChannel channel, String text) {
		String msg = String.format("<br>&nbsp;[%s]%s/%d：<br>&nbsp;%s", DateUtil.format(SystemClock.date()), getClientIp(channel), channel.getPeerConnections().size(), text);
		if("mute".equals(text)) {
			mute = !mute;
			log.info("chat mute: {}", mute);
		}else if(!mute){
			sendText(channel, msg, true);
			Long size = RedisConfig.lpush(RedisConfig.CACHE, key, msg);
			if(size > length) {
				RedisConfig.ltrim(RedisConfig.CACHE, key, 0, length-1);
			}
		}
	}

	@Override
	protected void onConnectEvent(WebSocketHttpExchange exchange, WebSocketChannel channel) {
		boolean history = NumberUtil.parseBoolean(RedisConfig.get("ws.chat.history"), true);
		List<String> list = RedisConfig.lrange(RedisConfig.CACHE, key, 0, length);
		int size = list==null ? 0 : list.size();
		log.info("history size: {}, history enabled: {}", size, history);
		if(size > 0 && history) {
			for(int i=size-1; i>=0; i--) {
				String msg = list.get(i);
				WebSockets.sendText(msg, channel, null);
			}
		}
	}
}
