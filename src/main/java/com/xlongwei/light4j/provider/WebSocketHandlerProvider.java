package com.xlongwei.light4j.provider;

import static io.undertow.Handlers.path;
import static io.undertow.Handlers.resource;
import static io.undertow.Handlers.websocket;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;

import com.networknt.handler.HandlerProvider;
import com.xlongwei.light4j.util.DateUtil;
import com.xlongwei.light4j.util.IdWorker.SystemClock;
import com.xlongwei.light4j.util.NumberUtil;
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

/**
 * web socket handler
 * @author xlongwei
 *
 */
@Slf4j
public class WebSocketHandlerProvider implements HandlerProvider {
	private final String key = "ws.chat";
	private final int length = 18;
    @Override
    public HttpHandler getHandler() {
        return path()
                .addPrefixPath("/ws/chat", websocket(new WebSocketConnectionCallback() {
                    @Override
                    public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
                        channel.getReceiveSetter().set(new AbstractReceiveListener() {
                        	boolean mute = false;
                            @Override
                            protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) {
                            	String txt = message.getData();
                                String msg = String.format("<br>&nbsp;[%s]%s/%d：<br>&nbsp;%s", DateUtil.format(SystemClock.date()), channel.getSourceAddress().getHostString(), channel.getPeerConnections().size(), txt);
                                if(!mute) {
                                	channel.getPeerConnections().parallelStream().forEach(c -> WebSockets.sendText(msg, c, null));
                                }
                                log.info(msg);
                                if(StringUtil.firstNotBlank(RedisConfig.get("ws.chat.mute"), "mute").equals(txt)) {
                                	mute = !mute;
                                	log.info("chat mute: {}", mute);
                                }else if("history".equals(txt)) {
                                	boolean history = !NumberUtil.parseBoolean(RedisConfig.get("ws.chat.history"), true);
                                	RedisConfig.set("ws.chat.history", String.valueOf(history));
                                	log.info("chat history: {}", history);
                                }
                                Long size = RedisConfig.lpush(RedisConfig.CACHE, key, msg);
                                if(size > length) {
                                	RedisConfig.ltrim(RedisConfig.CACHE, key, 0, length-1);
                                }
                            }
                        });
                        channel.resumeReceives();
                        boolean history = NumberUtil.parseBoolean(RedisConfig.get("ws.chat.history"), true);
                        List<String> list = RedisConfig.lrange(RedisConfig.CACHE, key, 0, length);
                        int size = list==null ? 0 : list.size();
                        log.info("ws chat on connect, history size: {}, history enabled: {}", size, history);
                        if(size > 0 && history) {
                        	for(int i=size-1; i>=0; i--) {
                        		String msg = list.get(i);
                        		WebSockets.sendText(msg, channel, null);
                        	}
                        }
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
										if(channel.getPeerConnections().isEmpty()) {
											log.info("tailer no connections and stop");
											tailer.stop();
											tailer = null;
										}else {
											channel.getPeerConnections().parallelStream().forEach(c -> WebSockets.sendText(line, c, null));
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
