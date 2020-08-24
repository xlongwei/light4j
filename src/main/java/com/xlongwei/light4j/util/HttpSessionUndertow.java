package com.xlongwei.light4j.util;

import java.util.Collections;
import java.util.Enumeration;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;

/**
 * 包装undertow的Session为apijson所需的HttpSession
 * @author xlongwei
 *
 */
public final class HttpSessionUndertow extends HttpSessionWrapper {
	private final Session session;
	private final HttpServerExchange exchange;
	public HttpSessionUndertow(Session session, HttpServerExchange exchange) {
		this.session = session;
		this.exchange = exchange;
	}
	@Override
	public long getCreationTime() {
		return session.getCreationTime();
	}
	@Override
	public String getId() {
		return session.getId();
	}
	@Override
	public long getLastAccessedTime() {
		return session.getLastAccessedTime();
	}
	@Override
	public void setMaxInactiveInterval(int interval) {
		session.setMaxInactiveInterval(interval);
	}
	@Override
	public int getMaxInactiveInterval() {
		return session.getMaxInactiveInterval();
	}
	@Override
	public Object getAttribute(String name) {
		return session.getAttribute(name);
	}
	@Override
	public Enumeration<String> getAttributeNames() {
		return Collections.enumeration(session.getAttributeNames());
	}
	@Override
	public void setAttribute(String name, Object value) {
		session.setAttribute(name, value);
	}
	@Override
	public void removeAttribute(String name) {
		session.removeAttribute(name);
	}
	@Override
	public void invalidate() {
		session.getAttributeNames().forEach(session::removeAttribute);
		session.invalidate(exchange);
	}
}