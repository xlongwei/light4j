package com.xlongwei.light4j.provider;

import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket抽象类
 * @author xlongwei
 *
 */
@Slf4j
public abstract class WebSocketAbstractCallback implements WebSocketConnectionCallback {
	private String name = getClass().getSimpleName().replace("WebSocket", "").replace("Callback", "").toLowerCase();

	@Override
	public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
		channel.getReceiveSetter().set(new AbstractReceiveListener() {
			@Override
			protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) {
				String text = message.getData();
				log.info("receive {} {}", channel.getPeerAddress(), text);
				onTextMessage(channel, text);
			}
		});
		log.info("connect {} {}", name, channel.getPeerAddress());
		channel.resumeReceives();
		onConnectEvent(exchange, channel);
	}
	
	/** 发送消息给客户端 */
	protected void sendText(WebSocketChannel channel, String text, boolean broadcast) {
		if(broadcast) {
			log.info("broadcast {} {}", channel.getPeerAddress(), text);
			channel.getPeerConnections().parallelStream().forEach(c -> WebSockets.sendText(text, c, null));
		}else {
			log.info("send {} {}", channel.getPeerAddress(), text);
			WebSockets.sendText(text, channel, null);
		}
	}
	
	protected void sendText(WebSocketChannel channel, String text) {
		sendText(channel, text, false);
	}

	/** 处理接受到的消息 */
	protected abstract void onTextMessage(WebSocketChannel channel, String text);
	
	/** 处理客户端连接事件 */
	protected void onConnectEvent(WebSocketHttpExchange exchange, WebSocketChannel channel) { }
}
