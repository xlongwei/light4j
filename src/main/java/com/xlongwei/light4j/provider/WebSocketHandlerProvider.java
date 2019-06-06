package com.xlongwei.light4j.provider;

import com.networknt.handler.HandlerProvider;
import com.xlongwei.light4j.util.RedisConfig;
import com.xlongwei.light4j.util.StringUtil;
import com.xlongwei.light4j.util.TaskUtil;

import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.CloseMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import lombok.extern.slf4j.Slf4j;

import static io.undertow.Handlers.path;
import static io.undertow.Handlers.resource;
import static io.undertow.Handlers.websocket;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;

/**
 * web socket handler
 * @author xlongwei
 *
 */
@Slf4j
public class WebSocketHandlerProvider implements HandlerProvider {
    @Override
    public HttpHandler getHandler() {
        return path()
                .addPrefixPath("/ws/chat", websocket(new WebSocketConnectionCallback() {
                    @Override
                    public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
                        channel.getReceiveSetter().set(new AbstractReceiveListener() {
                            @Override
                            protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) {
                            	Set<WebSocketChannel> peerConnections = channel.getPeerConnections();
                                String msg = channel.getSourceAddress()+"/"+peerConnections.size()+": "+message.getData();
                                peerConnections.parallelStream().forEach(c -> WebSockets.sendText(msg, c, null));
                                log.info(msg);
                            }
                        });
                        channel.resumeReceives();
                    }
                }))
                .addPrefixPath("/ws/logs", websocket(new WebSocketConnectionCallback() {
                	private Tailer tailer = null;
                	@Override
                	public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
                		log.info("tailer logs on connect");
                		if(tailer == null) {
                			File logs = new File(StringUtil.firstNotBlank(RedisConfig.get("logserver.file"), "logs/light4j.log"));
                			if(logs.exists()) {
	                			tailer = new Tailer(logs, StandardCharsets.UTF_8, new TailerListenerAdapter() {
									@Override
									public void handle(String line) {
										Set<WebSocketChannel> peerConnections = channel.getPeerConnections();
										if(channel.getPeerConnections().isEmpty()) {
											log.info("tailer no connections and stop");
											tailer.stop();
											tailer = null;
										}else {
											peerConnections.parallelStream().forEach(c -> WebSockets.sendText(line, c, null));
										}
									}
	                			}, 1000, true, false, 4096);
	                			TaskUtil.submit(tailer);
	                			log.info("tailer init and start");
                			}else {
                				log.info("tailer logs not exist");
                			}
                		}
                		channel.getReceiveSetter().set(new AbstractReceiveListener() {
							@Override
							protected void onCloseMessage(CloseMessage cm, WebSocketChannel channel) {
								if(tailer!=null && channel.getPeerConnections().isEmpty()) {
									tailer.stop();
									tailer = null;
									log.info("tailer stop and end");
								}
							}
                		});
                		channel.resumeReceives();
                	}
                	
                }))
                .addPrefixPath("/ws/", resource(new ClassPathResourceManager(WebSocketHandlerProvider.class.getClassLoader(), "public")));
    }
}
