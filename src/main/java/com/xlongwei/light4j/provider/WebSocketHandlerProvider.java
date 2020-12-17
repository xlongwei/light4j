package com.xlongwei.light4j.provider;

import static io.undertow.Handlers.path;
import static io.undertow.Handlers.resource;
import static io.undertow.Handlers.websocket;

import com.networknt.handler.HandlerProvider;

import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;

/**
 * web socket handler
 * @author xlongwei
 *
 */
public class WebSocketHandlerProvider implements HandlerProvider {
	@Override
	public HttpHandler getHandler() {
		return path()
				.addPrefixPath("/ws/chat", websocket(new WebSocketChatCallback()))
				.addPrefixPath("/ws/service", websocket(new WebSocketServiceCallback()))
				.addPrefixPath("/ws/ok", ResponseCodeHandler.HANDLE_200)
				.addPrefixPath("/ws/", resource(new ClassPathResourceManager(WebSocketHandlerProvider.class.getClassLoader(), "public")));
	}
}
