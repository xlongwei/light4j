package com.xlongwei.light4j.util;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpSession;

import org.apache.commons.codec.CharEncoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.networknt.service.SingletonServiceFactory;
import com.networknt.session.Session;
import com.networknt.session.SessionManager;
import com.networknt.utility.StringUtils;
import com.xlongwei.light4j.handler.ServiceHandler;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormData.FormValue;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.server.handlers.form.FormParserFactory.Builder;
import io.undertow.server.session.SessionAttachmentHandler;
import io.undertow.util.AttachmentKey;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.MimeMappings;
import io.undertow.util.Sessions;
import lombok.extern.slf4j.Slf4j;

/**
 * exhange工具类，获取请求参数，设置响应报文，获取会话等
 * @author xlongwei
 */
@Slf4j
public class HandlerUtil {
	public static final String MIMETYPE_JSON = MimeMappings.DEFAULT.getMimeType("json"), MIMETYPE_TXT = MimeMappings.DEFAULT.getMimeType("txt");
	private static final String TEXT = "text", XML = "xml", JSON = "json";
	private static final String SHOWAPI_USER_ID = "showapi_userId", SHOWAPI_USER_NAME = "showapi_userName";
	private static final SessionManager sessionManager = SingletonServiceFactory.getBean(SessionManager.class);
	private static final SessionAttachmentHandler sessionAttachmentHandler = sessionManager != null ? null
			: new SessionAttachmentHandler(
					SingletonServiceFactory.getBean(io.undertow.server.session.SessionManager.class),
					SingletonServiceFactory.getBean(io.undertow.server.session.SessionConfig.class));
	public static JSONObject ipsConfig = new JSONObject();
	public static Logger ipsConfigLogger = LoggerFactory.getLogger("ipsConfig");
	public static Map<String, AtomicInteger> ipsCounter = new ConcurrentHashMap<>();
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

	public static final AttachmentKey<Object> REQUEST_START_TIME = AttachmentKey.create(Object.class);

	/**
	 * 解析body为Map<String, Object>
	 * <br>Object可能是String、List<String>、FileItem、List<FileItem>
	 * @param exchange
	 */
	public static void parseBody(HttpServerExchange exchange) {
		Map<String, Object> body = new HashMap<>(4);
		Map<String, Deque<String>> params = exchange.getQueryParameters();
		for(Entry<String, Deque<String>> entry : params.entrySet()) {
			String param = entry.getKey();
			Deque<String> deque = entry.getValue();
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
				//MultiPartParserDefinition#93，exchange.addExchangeCompleteListener，在请求结束时关闭parser并删除临时文件
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
			        }else {
			        	InputStream inputStream = exchange.getInputStream();
						String string = StringUtils.trimToEmpty(HtmlUtil.string(inputStream));
			        	body.putAll(cn.hutool.http.HttpUtil.decodeParamMap(string, StandardCharsets.UTF_8));
			        }
			}else if(StringUtils.isBlank(contentType) || StringUtil.containsOneOfIgnoreCase(contentType, TEXT, JSON, XML)) {
				InputStream inputStream = exchange.getInputStream();
				String string = StringUtils.trimToEmpty(HtmlUtil.string(inputStream));
				if (string.length() > 0) {
					Map<String, Object> bodyMap = ConfigUtil.stringMapObject(string);
					if(bodyMap!=null && bodyMap.size()>0) {
						body.putAll(bodyMap);
					}else if(string.indexOf('=')>0){
						body.putAll(cn.hutool.http.HttpUtil.decodeParamMap(string, StandardCharsets.UTF_8));
					}
					body.put(BODYSTRING, string);
				}
			}else {
				log.info("not suppoert Content-Type: {}", contentType);
			}
		}catch(Exception e) {
			log.info("fail to parse body: {}", e.getMessage());
		}
		if(!body.isEmpty()) {
			Object obj = body.remove(BODYSTRING);
			log.info("body: {}", JSONObject.toJSONString(body));
			if(obj != null) {
				body.put(BODYSTRING, obj);
			}
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
	
	/** 支持参数或正文 */
	public static String getParamOrBody(HttpServerExchange exchange, String name) {
		String param = getObject(exchange, name, String.class);
		if(StringUtil.isBlank(param)) {
			param = getObject(exchange, BODYSTRING, String.class);
		}
		return param;
	}
	
	/** 获取正文字符串 */
	public static String getBodyString(HttpServerExchange exchange) {
		return getObject(exchange, BODYSTRING, String.class);
	}
	
	/** 获取请求参数名集合 */
	public static Set<String> getParamNames(HttpServerExchange exchange) {
		Map<String, Object> body = exchange.getAttachment(BODY);
		return body!=null ? body.keySet() : Collections.emptySet();
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
				}else if(String.class==clazz){
					return (T)obj.toString();
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
		if(exchange.isComplete()) {
			//LayuiHandler.captcha直接输出图片字节流，不响应json
			return;
		}
		boolean isShowapiRequest = isShowapiRequest(exchange);
		String response = isShowapiRequest ? ServiceHandler.BAD_REQUEST_SHOWAPI : ServiceHandler.BAD_REQUEST;
		Object resp = exchange.removeAttachment(HandlerUtil.RESP);
		String mimeType = MIMETYPE_JSON;
		if(resp != null) {
			if(resp instanceof Map) {
				//handler可能返回的是不可修改的Collections.singletonMap
				Map map = new LinkedHashMap<>((Map)resp);
				Object domain = map.get(UploadUtil.DOMAIN), path = map.get(UploadUtil.PATH);
				if(domain!=null && path!=null) {
					//接口响应了domain+path，添加响应参数url
					map.put("url", domain.toString()+path.toString());
				}
				if(isShowapiRequest) {
					map.put("ret_code", "0");
				}
				response = JSONObject.toJSONString(map);
			}else if(resp instanceof String) {
				response = (String)resp;
				mimeType = MIMETYPE_TXT;
			}
		}
		exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, mimeType);
		exchange.setStatusCode(200);
		log.info("res({}): {}", HandlerUtil.requestTime(exchange), response);
		exchange.getResponseSender().send(response);
	}
	
	/** 获取真实IP地址 */
	public static String getIp(HttpServerExchange exchange) {
		String[] ipHeaders = {"HTTP_X_FORWARDED_FOR","HTTP_CLIENT_IP","WL-Proxy-Client-IP","Proxy-Client-IP","X-Forwarded-For","X-Real-IP"};
		HeaderMap requestHeaders = exchange.getRequestHeaders();
		for(String ipHeader : ipHeaders) {
			String ipValue = requestHeaders.getFirst(ipHeader);
			if(!StringUtil.isBlank(ipValue) && !"unknown".equalsIgnoreCase(ipValue)) {
				int common = ipValue.indexOf(',');
				if(common > -1) {
					//clientip,proxy1,proxy2
					ipValue = ipValue.substring(0, common);
				}
				if(StringUtil.isIp(ipValue) && !"127.0.0.1".equals(ipValue)) {
					return ipValue;
				}
			}
		}
		return exchange.getSourceAddress().getAddress().getHostAddress();
	}
	
	/** 判断是否showapi用户 */
	public static boolean isShowapiRequest(HttpServerExchange exchange) {
		return StringUtils.isNotBlank(getParam(exchange, SHOWAPI_USER_ID));
	}
	
	/** 判断是否showapi客户 */
	public static boolean isShowapiClient(HttpServerExchange exchange) {
		return ConfigUtil.isClient(getParam(exchange, SHOWAPI_USER_ID));
	}
	
	/** 获取showapi用户名 */
	public static String getShowapiUserName(HttpServerExchange exchange) {
		String showapiUserName = getParam(exchange, SHOWAPI_USER_NAME);
		return StringUtils.trimToEmpty(showapiUserName);
	}
	
	/** 获取会话session */
	public static HttpSession getSession(HttpServerExchange exchange) throws Exception {
		return getSession(exchange, false);
	}

	/** 获取或创建会话session */
	public static HttpSession getOrCreateSession(HttpServerExchange exchange) throws Exception {
		return getSession(exchange, true);
	}
	
	/** 获取或创建会话session */
	public static HttpSession getSession(HttpServerExchange exchange, boolean create) throws Exception {
		if(sessionManager != null) {
			Session session = sessionManager.getSession(exchange);
			if(session == null && create) {
				session = sessionManager.createSession(exchange);
			}
			return new HttpSessionNetworknt(session);
		}else {
			sessionAttachmentHandler.handleRequest(exchange);
			return new HttpSessionUndertow(create ? Sessions.getOrCreateSession(exchange) : Sessions.getSession(exchange), exchange);
		}
	}
	
	/** true处理请求 false提示或拒绝 */
	public static boolean ipsConfig(HttpServerExchange exchange, String name) {
		if(ipsConfig!=null && !ipsConfig.isEmpty()) {
			String ip = getIp(exchange);
			if(!StringUtil.splitContains(ipsConfig.getString("whites"), ip)) {
				if(!StringUtil.splitContains(ipsConfig.getString("frees"), name)) {
					AtomicInteger counter = ipsCounter.get(ip);
					if(counter==null) {
						ipsCounter.put(ip, new AtomicInteger(1));
					}else {
						int count = counter.getAndIncrement();
						int limits = ipsConfig.getIntValue("limits");
						ipsConfigLogger.debug("ip={} count={}", ip, count);
						if(limits>0 && count>=limits) {
							if(count%100 == 0) {
								log.info("ipsConfig ban ip={} count={}", ip, count);
							}
							return false;
						}
					}
				}
			}
		}
		return true;
	}
	/** 手动更新ipsConfig */
	public static void ipsConfigUpdate() {
		String configKey = "service.controller.ips.config";
		ipsConfig = JsonUtil.parseNew(RedisConfig.get(System.getProperty(configKey, configKey)));
		log.info("ipsConfig limits={}", ipsConfig.getIntValue("limits"));
	}
	/** 定时清除ip计数器 */
	public static void ipsCounterClear(String ip) {
		if(ip==null) {
			ipsCounter.entrySet().forEach(entry -> {
				int counter = entry.getValue().get(), count = counter/100;
				if(count>0) {
					String key = entry.getKey();
					log.info("serviceCount {}={} counter={}", key, count, counter);
					for(int i=0;i<count;i++) {
						ServiceHandler.serviceCount(key);
					}
				}
			});
			ipsCounter.clear();
		}else {
			ipsCounter.remove(ip);
		}
		log.info("ipsCounter clear done");
	}
	public static void requestStartTime(HttpServerExchange exchange) {
		if(exchange.getRequestStartTime()==-1) {
			exchange.putAttachment(REQUEST_START_TIME, System.nanoTime());
		}
	}
	public static long requestTime(HttpServerExchange exchange) {
		long requestStartTime = exchange.getRequestStartTime();
		if(requestStartTime == -1) {
			Object obj = exchange.getAttachment(REQUEST_START_TIME);
			if(obj != null) {
				requestStartTime = (Long)obj;
			}else{
				return 0;
			}
		}
		return (System.nanoTime() - requestStartTime)/1000;
	}
}
