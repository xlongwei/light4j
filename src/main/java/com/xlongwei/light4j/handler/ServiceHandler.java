package com.xlongwei.light4j.handler;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONAware;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.omg.CORBA.IntHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.networknt.config.Config;
import com.networknt.handler.LightHttpHandler;
import com.networknt.utility.StringUtils;
import com.xlongwei.light4j.util.ConfigUtil;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.RedisConfig;
import com.xlongwei.light4j.util.TokenCounter;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import io.undertow.util.Headers;
import io.undertow.util.MimeMappings;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServiceHandler implements LightHttpHandler {
	public static final String badRequest = "{\"status\":\"200\", \"error\":\"bad request\"}";
	private Map<String, AbstractHandler> handlers = new HashMap<>();
	private static ServiceCounter serviceCounter = new ServiceCounter(64, TimeUnit.SECONDS.toMillis(18));
	
	public ServiceHandler() {
		List<AbstractHandler> list = HandlerUtil.scanHandlers(AbstractHandler.class, getClass().getPackage().getName()+".service");
		for(AbstractHandler handler : list) {
			String name = handler.name();
			handlers.put(name, handler);
			log.info("load {} => {}", name, handler.getClass().getName());
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
				HandlerUtil.parseBody(exchange);
				handler.handleRequest(exchange);
				serviceCounter.count("service", handler.name());
			}
		}
		exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, MimeMappings.DEFAULT.getMimeType("json"));
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
	
	public static abstract class AbstractHandler implements LightHttpHandler {
		public static final AttachmentKey<String> PATH = AttachmentKey.create(String.class);//子服务路径，存在时反射调用子服务方法，否则子类覆盖handleRequest方法
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
	}
	
	private static class ServiceCounter extends TokenCounter {
		public ServiceCounter(int expireCount, long expireTime) {
			this.expireCount = expireCount;
			this.expireTime = expireTime;
			init();
		}
		@Override
		protected void merge(TokenCount tokenCount) {
			String key = tokenCount.token+tokenCount.day;
			String string = RedisConfig.get(key);
			Map<String, Integer> counts = ConfigUtil.stringMapInteger(string);
			if(counts == null) {
				counts = new HashMap<>(tokenCount.counts.size());
				for(Entry<String, IntHolder> count : tokenCount.counts.entrySet()) {
					counts.put(count.getKey(), count.getValue().value);
				}
			}else {
				for(Entry<String, IntHolder> count : tokenCount.counts.entrySet()) {
					Integer value = counts.get(count.getKey());
					if(value == null) {
						counts.put(count.getKey(), count.getValue().value);
					}else {
						counts.put(count.getKey(), value+count.getValue().value);
					}
				}
			}
			String value = JSONObject.toJSONString(counts);
			log.info("update service counter key: {} count: {}", key, value);
			RedisConfig.set(key, value);
		}
	}
}
