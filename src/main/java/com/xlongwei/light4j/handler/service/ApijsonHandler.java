package com.xlongwei.light4j.handler.service;

import java.util.Collections;
import java.util.Enumeration;

import javax.servlet.http.HttpSession;

import com.networknt.utility.StringUtils;
import com.xlongwei.light4j.apijson.DemoApplication;
import com.xlongwei.light4j.apijson.DemoController;
import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.HandlerUtil;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.InMemorySessionManager;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionAttachmentHandler;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionCookieConfig;
import io.undertow.server.session.SessionManager;
import io.undertow.util.Methods;
import io.undertow.util.Sessions;

public class ApijsonHandler extends AbstractHandler {
	private static final DemoController apijson = DemoApplication.apijson;
	private static final SessionManager sessionManager = new InMemorySessionManager("apijson");
	private static final SessionConfig sessionConfig = new SessionCookieConfig();
	private static final SessionAttachmentHandler sessionAttachmentHandler = new SessionAttachmentHandler(sessionManager, sessionConfig);

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		if(!DemoApplication.apijsonEnabled || !Methods.POST.equals(exchange.getRequestMethod())) {
			HandlerUtil.setResp(exchange, Collections.singletonMap("error", !DemoApplication.apijsonEnabled ? "apijson not enabled" : "POST is required"));
			return;
		}
		String path = exchange.getAttachment(AbstractHandler.PATH), request = HandlerUtil.getBodyString(exchange);
		if(StringUtils.isBlank(path) || (StringUtils.isBlank(request) && !"logout".equals(path))) {
			HandlerUtil.setResp(exchange, Collections.singletonMap("error", StringUtils.isBlank(path) ? "apijson/{path} is required" : "body is required"));
			return;
		}
		sessionAttachmentHandler.handleRequest(exchange);
		HttpSession session = new HttpSessionWrapper(Sessions.getOrCreateSession(exchange), exchange);
		String json = null;
		switch(path) {
		case "get": json = apijson.get(request, session); break;
		case "head": json = apijson.head(request, session); break;
		case "gets": json = apijson.gets(request, session); break;
		case "heads": json = apijson.heads(request, session); break;
		case "post": json = apijson.post(request, session); break;
		case "put": json = apijson.put(request, session); break;
		case "delete": json = apijson.delete(request, session); break;
		case "reload": json = apijson.reload(request).toJSONString(); break;
		case "postVerify": json = apijson.postVerify(request).toJSONString(); break;
		case "getVerify": json = apijson.getVerify(request).toJSONString(); break;
		case "headVerify": json = apijson.headVerify(request).toJSONString(); break;
		case "login": json = apijson.login(request, session).toJSONString(); break;
		case "logout": json = apijson.logout(session).toJSONString(); break;
		case "register": json = apijson.register(request).toJSONString(); break;
		case "putPassword": json = apijson.putPassword(request).toJSONString(); break;
		case "putBalance": json = apijson.putBalance(request, session).toJSONString(); break;
		case "invokeMethod": json = apijson.invokeMethod(request).toJSONString(); break;
		default: 
			HandlerUtil.setResp(exchange, Collections.singletonMap("error", "apijson/"+path+" not supported"));
			return;
		}
		if(json != null) {
			HandlerUtil.setResp(exchange, Collections.singletonMap("json", json));
		}
	}

	/**
	 * 包装undertow的Session为apijson所需的HttpSession
	 * @author xlongwei
	 *
	 */
	public static class HttpSessionWrapper implements HttpSession {
		private Session session;
		private HttpServerExchange exchange;
		public HttpSessionWrapper(Session session, HttpServerExchange exchange) {
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
		@Override
		public boolean isNew() {
			return true;
		}
		@Deprecated public javax.servlet.http.HttpSessionContext getSessionContext() { return null; }
		@Deprecated public javax.servlet.ServletContext getServletContext() { return null; }
		@Deprecated public Object getValue(String name) { return null; }
		@Deprecated public String[] getValueNames() { return null; }
		@Deprecated public void putValue(String name, Object value) { }
		@Deprecated public void removeValue(String name) { }
	}
}
