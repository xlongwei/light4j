package com.xlongwei.light4j.handler.service;

import java.util.Collections;

import javax.servlet.http.HttpSession;

import com.alibaba.fastjson.JSONObject;
import com.networknt.utility.StringUtils;
import com.xlongwei.light4j.apijson.DemoApplication;
import com.xlongwei.light4j.apijson.DemoController;
import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.JsonUtil;

import org.apache.commons.lang3.ArrayUtils;

import apijson.framework.APIJSONSQLExecutor;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApijsonHandler extends AbstractHandler {
	private static final DemoController apijson = DemoApplication.apijson;
	private static final String[] ALLOW_EMPTY_BODY_METHODS = {"logout", "method_list"};
	
	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		if(!DemoApplication.apijsonEnabled || !Methods.POST.equals(exchange.getRequestMethod())) {
			HandlerUtil.setResp(exchange, Collections.singletonMap("error", !DemoApplication.apijsonEnabled ? "apijson not enabled" : "POST is required"));
			return;
		}
		String path = exchange.getAttachment(AbstractHandler.PATH), request = HandlerUtil.getBodyString(exchange);
		apijson(path, request, exchange);
	}

	public static void apijson(String path, String request, HttpServerExchange exchange) throws Exception {
		if(StringUtils.isBlank(path) || (StringUtils.isBlank(request) && !ArrayUtils.contains(ALLOW_EMPTY_BODY_METHODS, path))) {
			HandlerUtil.setResp(exchange, Collections.singletonMap("error", StringUtils.isBlank(path) ? "apijson/{path} is required" : "body is required"));
			return;
		}
		HttpSession session = HandlerUtil.getOrCreateSession(exchange);
		String json = null;
		int borrowedConnections = APIJSONSQLExecutor.borrowedConnections.get();
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
		case "method_list": json = apijson.listMethod(request).toJSONString(); break;
		case "method_invoke": json = apijson.invokeMethod(request).toJSONString(); break;
		default: 
			HandlerUtil.setResp(exchange, Collections.singletonMap("error", "apijson/"+path+" not supported"));
			return;
		}
		//全局监控apijson是否有连接泄露，apijson不抛异常出来因此这里不必try-finally
		if(APIJSONSQLExecutor.borrowedConnections.get() > borrowedConnections) {
			log.error("apijson connection leak, path={} borrowedConnections={} -> {}", path, borrowedConnections, APIJSONSQLExecutor.borrowedConnections.get());
			try{
				APIJSONSQLExecutor.threadConnection.get().close();
				log.info("apijson connection release, returned to {}", APIJSONSQLExecutor.borrowedConnections.decrementAndGet());
				APIJSONSQLExecutor.threadConnection.remove();
			}catch(Exception e) {
				log.info("{} {}", e.getClass().getSimpleName(), e.getMessage());
			}
		}
		if(json != null) {
			String response = json;
			if(HandlerUtil.isShowapiRequest(exchange)) {
				JSONObject obj = JsonUtil.parse(json);
				if(obj != null) {
					obj.put("ret_code", "0");
					response = obj.toJSONString();
				}
			}
			exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, HandlerUtil.MIMETYPE_JSON);
			exchange.setStatusCode(200);
			log.info("res({}): {}", HandlerUtil.requestTime(exchange), response);
			exchange.getResponseSender().send(response);
		}
	}
}
