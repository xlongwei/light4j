package com.xlongwei.light4j.provider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.xlongwei.light4j.util.StringUtil;

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
	private Map<String, String> serverMapClients = new ConcurrentHashMap<>();
	private static String[] ipHeaders = {"X-Forwarded-For", "X-Real-IP"};

	@Override
	public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
		channel.getReceiveSetter().set(new AbstractReceiveListener() {
			@Override
			protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) {
				String text = message.getData();
				log.info("receive {} <= {} {}", getServerPort(channel), getClientPort(channel), text);
				onTextMessage(channel, text);
			}
		});
		String serverPort = getServerPort(channel);
		String clientPort = getClientPort(exchange, channel);
		log.info("connect {} {} <=> {}", name, serverPort, clientPort);
		serverMapClients.put(serverPort, clientPort);
		channel.resumeReceives();
		onConnectEvent(exchange, channel);
	}
	
	/** /127.0.0.1:61642 */
	public static String getClientPort(WebSocketHttpExchange exchange, WebSocketChannel channel) {
		String clientPort = channel.getSourceAddress().toString();
		String clientIp = null;
		for(String ipHeader : ipHeaders) {
			String ipValue = exchange.getRequestHeader(ipHeader);
			if(!StringUtil.isBlank(ipValue) && ipValue.indexOf('.')>0) {
				int p = ipValue.indexOf(',');
				if(p>0) {
					ipValue = ipValue.substring(0, p);
				}
				if(StringUtil.isIp(ipValue) && !"127.0.0.1".equals(ipValue)) {
					clientIp = ipValue;
				}
			}
			if(clientIp != null) {
				break;
			}
		}
		return clientIp==null ? clientPort : "/"+clientIp+clientPort.substring(clientPort.indexOf(':'));
	}
	
	/** 发送消息给客户端 */
	protected void sendText(WebSocketChannel channel, String text, boolean broadcast) {
		if(broadcast) {
			log.info("broadcast {} => {} {}", getServerPort(channel), getClientPort(channel), text);
			channel.getPeerConnections().parallelStream().forEach(c -> WebSockets.sendText(text, c, null));
		}else {
			log.info("send {} => {} {}", getServerPort(channel), getClientPort(channel), text);
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
	
	/** /127.0.0.1:61642 */
	protected String getServerPort(WebSocketChannel channel) {
		return channel.getDestinationAddress().toString();
	}
	
	/** /127.0.0.1:61642 */
	protected String getClientPort(WebSocketChannel channel) {
		return serverMapClients.get(getServerPort(channel));
	}
	
	protected String getClientIp(WebSocketChannel channel) {
		String clientPort = getClientPort(channel);
		return StringUtil.isBlank(clientPort) ? "" : clientPort.substring(1, clientPort.lastIndexOf(':'));
	}
}
