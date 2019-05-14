package com.xlongwei.light4j.util;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.CharEncoding;
import org.jose4j.json.internal.json_simple.JSONObject;

import com.networknt.utility.StringUtils;
import com.xlongwei.light4j.handler.ServiceHandler;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.lukehutch.fastclasspathscanner.scanner.ScanResult;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormData.FormValue;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.server.handlers.form.FormParserFactory.Builder;
import io.undertow.util.AttachmentKey;
import io.undertow.util.Headers;
import io.undertow.util.MimeMappings;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xlongwei
 *
 */
@Slf4j
public class HandlerUtil {
	public static final String MIMETYPE_JSON = MimeMappings.DEFAULT.getMimeType("json"), MIMETYPE_TXT = MimeMappings.DEFAULT.getMimeType("txt");
	private static final String TEXT = "text", XML = "xml", JSON = "json";
	private static final String SHOWAPI_USER_ID = "showapi_userId";
	/**
	 * 请求参数和正文
	 */
	public static final AttachmentKey<Map<String, Object>> BODY = AttachmentKey.create(Map.class);
	/**
	 * 请求正文字符串
	 */
	public static final String BODYSTRING = "BODYSTRING";
	/**
	 * 响应对象
	 */
	public static final AttachmentKey<Object> RESP = AttachmentKey.create(Object.class);

	@SuppressWarnings("unchecked")
	public static <T> List<T> scanHandlers(Class<T> clazz, String scanSpec) {
		FastClasspathScanner scanner = new FastClasspathScanner(scanSpec);
		ScanResult scanResult = scanner.scan();
		List<String> list = scanResult.getNamesOfAllClasses();
		List<T> handlers = new ArrayList<>();
		for(String className : list) {
			if(!className.startsWith(scanSpec)) {
				continue;
			}
			try {
				Class<?> clz = Class.forName(className);
				if(clazz.isAssignableFrom(clz)) {
					T handler = (T)newInstance(clz);
					handlers.add(handler);
				}
			}catch(Exception e) {
				log.info("failed: {}, ex: {}", className, e.getMessage());
			}
		}
		return handlers;
	}
	
	public static <T> T newInstance(Class<T> clazz) {
		try {
			return clazz.getConstructor().newInstance();
		}catch(Exception e) {
			log.info("fail to instance:{}, ex:{}", clazz, e.getMessage());
			return null;
		}
	}
	
	/**
	 * 解析body为Map<String, Object>
	 * <br>Object可能是String、List<String>、FileItem、List<FileItem>
	 * @param exchange
	 */
	public static void parseBody(HttpServerExchange exchange) {
		Map<String, Object> body = new HashMap<>(4);
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
			boolean isForm = StringUtils.isNotBlank(contentType) && (contentType.startsWith("multipart/form-data") || contentType.startsWith("application/x-www-form-urlencoded"));
			if(isForm) {
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
		            			list.add(formValue.isFileItem() ? formValue : formValue.getValue());
		            		}
		            		body.put(name, list);
		            	}else {
		            		FormValue formValue = deque.getFirst();
		            		body.put(name, formValue.isFileItem() ? formValue : formValue.getValue());
		            	}
		            }
		        }
			}else if(StringUtils.isBlank(contentType) || StringUtil.containsOneOfIgnoreCase(contentType, TEXT, JSON, XML)) {
				InputStream inputStream = exchange.getInputStream();
				String string = StringUtils.inputStreamToString(inputStream, StandardCharsets.UTF_8);
				if (string != null) {
					string = string.trim();
					String lp = "{";
					if (string.startsWith(lp)) {
						Map<String, Object> readValue = ConfigUtil.stringMapObject(string);
						if(readValue!=null && readValue.size()>0) {
							body.putAll(readValue);
						}
					}else {
						body.put(BODYSTRING, string);
					}
				}
			}else {
				log.info("not suppoert Content-Type: {}", contentType);
			}
		}catch(Exception e) {
			log.info("fail to parse body: {}", e.getMessage());
		}
		if(!body.isEmpty()) {
			log.info("body: {}", body);
			exchange.putAttachment(BODY, body);
		}
	}
	
	/** 获取请求参数 */
	public static String getParam(HttpServerExchange exchange, String name) {
		return getObject(exchange, name, String.class);
	}
	
	/** FormValue包含fileName+FileItem */
	public static FormValue getFile(HttpServerExchange exchange, String name) {
		return getObject(exchange, name, FormValue.class);
	}
	
	/** 支持从缓存获取值 */
	public static String getParam(HttpServerExchange exchange, String name, String cacheKey) {
		String param = getParam(exchange, name);
		if(StringUtils.isBlank(param)) {
			param = getParam(exchange, cacheKey);
			if(StringUtils.isNotBlank(param)) {
				param = RedisConfig.get(param);
			}
		}
		return param;
	}
	
	/**
	 * @param obj
	 * @param clazz 支持String、FormValue、List
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> T getObject(HttpServerExchange exchange, String name, Class<T> clazz) {
		Map<String, Object> body = exchange.getAttachment(BODY);
		if(body != null) {
			Object obj = body.get(name);
			if(obj != null) {
				Class<? extends Object> clz = obj.getClass();
				if(clazz==clz || clazz.isAssignableFrom(clz)) {
					return (T)obj;
				}else if(List.class.isAssignableFrom(clz)) {
					obj = ((List)obj).get(0);
					clz = obj.getClass();
					if(clazz==clz || clazz.isAssignableFrom(clz)) {
						return (T)obj;
					}
				}
			}
		}
		return null;
	}
	
	/** 仅支持map，其他类型需手动响应 */
	public static void setResp(HttpServerExchange exchange, Map<String, ?> map) {
		exchange.putAttachment(HandlerUtil.RESP, map);
	}
	
	/** 仅支持json，其他类型需手动响应 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void sendResp(HttpServerExchange exchange) {
		String response = ServiceHandler.BAD_REQUEST;
		Object resp = exchange.removeAttachment(HandlerUtil.RESP);
		String mimeType = MIMETYPE_JSON;
		if(resp != null) {
			if(resp instanceof Map) {
				Map map = (Map)resp;
				Object domain = map.get(UploadUtil.DOMAIN), path = map.get(UploadUtil.PATH);
				if(domain!=null && path!=null) {
					//接口响应了domain+path，添加响应参数url
					map.put("url", domain.toString()+path.toString());
				}
				if(map.containsKey(SHOWAPI_USER_ID)) {
					map.put("ret_code", map.containsKey("error") ? "1" : "0");
				}
				response = JSONObject.toJSONString(map);
			}else if(resp instanceof String) {
				response = (String)resp;
				mimeType = MIMETYPE_TXT;
			}
		}
		exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, mimeType);
		exchange.setStatusCode(200);
		log.info("response: {}", response);
		exchange.getResponseSender().send(response);
	}
}
