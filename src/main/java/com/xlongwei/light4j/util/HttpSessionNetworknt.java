package com.xlongwei.light4j.util;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;

import com.networknt.session.Session;

/**
 * 包装networknt的Session为apijson所需的HttpSession
 * @author xlongwei
 *
 */
public final class HttpSessionNetworknt extends HttpSessionWrapper {
	private final Session session;
	public HttpSessionNetworknt(Session session) {
		this.session = session;
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
		new HashSet<>(session.getAttributeNames()).forEach(session::removeAttribute);
	}
}