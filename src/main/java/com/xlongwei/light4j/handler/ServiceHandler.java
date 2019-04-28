package com.xlongwei.light4j.handler;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.CharEncoding;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONAware;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.handler.LightHttpHandler;
import com.networknt.utility.StringUtils;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.lukehutch.fastclasspathscanner.scanner.ScanResult;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormData.FileItem;
import io.undertow.server.handlers.form.FormData.FormValue;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.server.handlers.form.FormParserFactory.Builder;
import io.undertow.util.AttachmentKey;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

public class ServiceHandler implements LightHttpHandler {
	public static final String badRequest = "{\"status\":\"200\", \"error\":\"bad request\"}";
	private Logger log = LoggerFactory.getLogger(getClass());
	private Map<String, AbstractHandler> handlers = new HashMap<>();
	
	public ServiceHandler() {
		FastClasspathScanner scanner = new FastClasspathScanner(getClass().getPackage().getName()+".service");
		ScanResult scanResult = scanner.scan();
		List<String> list = scanResult.getNamesOfSubclassesOf(AbstractHandler.class);
		for(String className : list) {
			try {
				Class<?> clazz = Class.forName(className);
				if(AbstractHandler.class.isAssignableFrom(clazz)) {
					AbstractHandler handler = (AbstractHandler)clazz.newInstance();
					String name = handler.name();
					handlers.put(name, handler);
					log.info("load {} => {}", name, className);
				}
			}catch(Exception e) {
				log.info("failed: {}, ex: {}", className, e.getMessage());
			}
		}
	}

	@Override
	@SuppressWarnings("rawtypes")
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }
		Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();
		String service = queryParameters.remove("*").getFirst();
		log.info("{} {}", exchange.getRequestMethod(), exchange.getRequestURI());
		int dot = service.indexOf('.');
		String[] split = StringUtils.split(dot>0 ? service.substring(0, dot) : service, "/");
		if(split.length > 0) {
			AbstractHandler handler = handlers.get(split[0]);
			if(handler != null) {
				String path = split.length>1 ? split[1] : "";
				exchange.putAttachment(AbstractHandler.PATH, path);
				Map<String, Object> body = body(exchange);
				if(!body.isEmpty()) {
					log.info("body: {}", body);
					exchange.putAttachment(AbstractHandler.BODY, body);
				}
				handler.handleRequest(exchange);
			}
		}
		exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
		exchange.setStatusCode(200);
		String response = badRequest;
		Object resp = exchange.getAttachment(AbstractHandler.RESP);
		if(resp != null) {
			if(resp instanceof String) {
				response = (String)resp;
			}else if(resp instanceof JSONAware) {
				response = ((JSONAware)resp).toJSONString();
			}else if(resp instanceof Map) {
				response = JSONObject.toJSONString((Map)resp);
			}else if(resp instanceof Collection) {
				response = JSONArray.toJSONString((Collection)resp);
			}else {
				response = Config.getInstance().getMapper().writeValueAsString(resp);
			}
		}
		log.info("result: {}", response);
		exchange.getResponseSender().send(response);
	}
	
	//Object可能是String、List<String>、FileItem、List<FileItem>
	private Map<String, Object> body(HttpServerExchange exchange) {
		Map<String, Object> body = new HashMap<>();
		Map<String, Deque<String>> params = exchange.getQueryParameters();
		for(String param : params.keySet()) {
			Deque<String> deque = params.get(param);
			if(deque.size() > 1) {
				body.put(param, new ArrayList<>(deque));
			}else {
				body.put(param, deque.getFirst());
			}
		}
		String contentType = exchange.getRequestHeaders().getFirst(Headers.CONTENT_TYPE);
		try {
			exchange.startBlocking();//参考BodyHandler
			if(StringUtils.isNotBlank(contentType) && (contentType.startsWith("multipart/form-data") || contentType.startsWith("application/x-www-form-urlencoded"))) {
				Builder builder = FormParserFactory.builder();
				builder.setDefaultCharset(CharEncoding.UTF_8);
				FormParserFactory formParserFactory = builder.build();
		        FormDataParser parser = formParserFactory.createParser(exchange);
		        if (parser != null) {
		            FormData formData = parser.parseBlocking();
		            for(String name : formData) {
		            	Deque<FormValue> deque = formData.get(name);
		            	if(deque.size() > 1) {
		            		List<Object> list = new ArrayList<>();
		            		for(FormValue formValue : deque) {
		            			list.add(formValue.isFileItem() ? formValue.getFileItem() : formValue.getValue());
		            		}
		            		body.put(name, list);
		            	}else {
		            		FormValue formValue = deque.getFirst();
		            		if(formValue.isFileItem()) {
		            			body.put(name, formValue.getFileItem());
		            		}else {
		            			body.put(name, formValue.getValue());
		            		}
		            	}
		            }
		        }
			}else if(StringUtils.isBlank(contentType) || contentType.contains("text") || contentType.contains("json")) {
				InputStream inputStream = exchange.getInputStream();
				String string = StringUtils.inputStreamToString(inputStream, StandardCharsets.UTF_8);
				if (string != null) {
					string = string.trim();
					if (string.startsWith("{")) {
						@SuppressWarnings("unchecked")
						Map<String, Object> readValue = (Map<String, Object>)Config.getInstance().getMapper().readValue(string, new TypeReference<Map<String, Object>>() {});
						if(readValue!=null && readValue.size()>0) {
							body.putAll(readValue);
						}
					}
				}
			}else {
				log.info("not suppoert Content-Type: {}", contentType);
			}
		}catch(Exception e) {
			log.info("fail to parse body: {}", e.getMessage());
		}
		return body;
	}
	
	public static abstract class AbstractHandler implements LightHttpHandler {
		public static final AttachmentKey<String> PATH = AttachmentKey.create(String.class);//子服务路径，存在时反射调用子服务方法，否则子类覆盖handleRequest方法
		public static final AttachmentKey<Map<String, Object>> BODY = AttachmentKey.create(Map.class);//请求参数和正文
		public static final AttachmentKey<Object> RESP = AttachmentKey.create(Object.class);//响应对象，不存在时响应bad request，支持String、JSONObject、JSONArray、Map、Collection等
		protected Logger log = LoggerFactory.getLogger(getClass());
		private Map<String, Method> methods = new HashMap<>();
		public AbstractHandler() {
			Method[] declaredMethods = getClass().getDeclaredMethods();
			for(Method method : declaredMethods) {
				if("name".equals(method.getName())) continue;
				if(Modifier.isPublic(method.getModifiers())) {
					// 暂未检查returnType和parameterTypes
					methods.put(method.getName(), method);
				}
			}
		}
		//ValidateHandler => validate
		public String name() {
			String simpleName = getClass().getSimpleName();
			int handler = simpleName.indexOf("Handler");
			return handler>0 ? simpleName.substring(0, handler).toLowerCase() : simpleName;
		}

		@Override
		public void handleRequest(HttpServerExchange exchange) throws Exception {
			String path = exchange.getAttachment(PATH);
			Method method = methods.get(path);
			if(method != null) {
				try {
					method.invoke(this, exchange);
					return;
				}catch(Exception e) {
					log.warn("service handle failed, path: {}, ex: {}", path, e.getMessage());
				}
			}
			exchange.getResponseSender().send(badRequest);
		}
		
		protected String getParam(HttpServerExchange exchange, String name) {
			Map<String, Object> body = exchange.getAttachment(AbstractHandler.BODY);
			return body==null ? null : getObject(body.get(name), String.class);
		}
		
		protected FileItem getFile(HttpServerExchange exchange, String name) {
			Map<String, Object> body = exchange.getAttachment(AbstractHandler.BODY);
			return body==null ? null : getObject(body.get(name), FileItem.class);
		}
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		private <T> T getObject(Object obj, Class<T> clazz) {
			if(obj != null) {
				if(clazz==obj.getClass()) {
					return (T)obj;
				}else if(List.class.isAssignableFrom(obj.getClass())) {
					obj = ((List)obj).get(0);
					if(clazz==obj.getClass()) {
						return (T)obj;
					}
				}
			}
			return null;
		}
	}
}
