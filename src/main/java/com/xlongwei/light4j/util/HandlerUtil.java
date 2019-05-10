package com.xlongwei.light4j.util;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.CharEncoding;

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
import lombok.extern.slf4j.Slf4j;

/**
 * @author xlongwei
 *
 */
@Slf4j
public class HandlerUtil {
	private static final String TEXT = "text";
	private static final String XML = "xml";
	private static final String JSON = "json";
	/**
	 * 请求参数和正文
	 */
	public static final AttachmentKey<Map<String, Object>> BODY = AttachmentKey.create(Map.class);
	/**
	 * 请求正文字符串
	 */
	public static final String BODYSTRING = "BODYSTRING";

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
	
	public static String getParam(HttpServerExchange exchange, String name) {
		return getObject(exchange, name, String.class);
	}
	
	public static FileItem getFile(HttpServerExchange exchange, String name) {
		return getObject(exchange, name, FileItem.class);
	}
	
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
	 * @param clazz 支持String、FileItem、List
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> T getObject(HttpServerExchange exchange, String name, Class<T> clazz) {
		Map<String, Object> body = exchange.getAttachment(BODY);
		if(body != null) {
			Object obj = body.get(name);
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
		}
		return null;
	}
}
