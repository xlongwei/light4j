package com.xlongwei.light4j.util;

import javax.servlet.http.HttpSession;

/**
 * 包装具体的Session为apijson所需的HttpSession
 * @author xlongwei
 *
 */
public abstract class HttpSessionWrapper implements HttpSession {
	@Override public boolean isNew() { return true; }
	@Deprecated @Override public javax.servlet.http.HttpSessionContext getSessionContext() { return null; }
	@Deprecated @Override public javax.servlet.ServletContext getServletContext() { return null; }
	@Deprecated @Override public Object getValue(String name) { return null; }
	@Deprecated @Override public String[] getValueNames() { return null; }
	@Deprecated @Override public void putValue(String name, Object value) { }
	@Deprecated @Override public void removeValue(String name) { }
}