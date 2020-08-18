package com.xlongwei.light4j.openapi.extend;

import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.utility.ModuleRegistry;

import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

/**
 * MiddlewareHandler adapter
 * @author xlongwei
 *
 */
public class DummyMiddlewareHandler implements MiddlewareHandler {
	private HttpHandler next;
	
	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		Handler.next(exchange, next);
	}

	@Override
	public HttpHandler getNext() {
		return next;
	}

	@Override
	public MiddlewareHandler setNext(HttpHandler next) {
		Handlers.handlerNotNull(next);
        this.next = next;
        return this;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public void register() {
		ModuleRegistry.registerModule(getClass().getName(), null, null);
	}

}
