package com.xlongwei.light4j.handler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.networknt.handler.LightHttpHandler;
import com.networknt.utility.StringUtils;
import com.xlongwei.light4j.util.ConfigUtil;
import com.xlongwei.light4j.util.DateUtil;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.PathEndpointSource;
import com.xlongwei.light4j.util.RedisConfig;
import com.xlongwei.light4j.util.StringUtil;
import com.xlongwei.light4j.util.TaskUtil;
import com.xlongwei.light4j.util.TokenCounter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import lombok.extern.slf4j.Slf4j;

/**
 * service handler
 * @author xlongwei
 *
 */
@Slf4j
public class ServiceHandler implements LightHttpHandler {
	public static final String BAD_REQUEST = "{\"error\":\"bad request\"}";
	public static final String BAD_REQUEST_SHOWAPI = "{\"error\":\"bad request\",\"ret_code\":\"0\"}";
	public static final Map<String, AbstractHandler> handlers = new HashMap<>();
	public static volatile boolean serviceCount = true;
	private static final ServiceCounter serviceCounter = new ServiceCounter(64, TimeUnit.SECONDS.toMillis(18));
	
	public ServiceHandler() {
		String pkg = getClass().getPackage().getName()+".service";
		Set<Class<?>> list = ClassUtil.scanPackageBySuper(pkg, AbstractHandler.class);
		for(Class<?> clazz : list) {
			AbstractHandler handler = (AbstractHandler)ReflectUtil.newInstanceIfPossible(clazz);
			String name = handler.name();
			handlers.put(name, handler);
			log.info("load {} => {}", name, handler.getClass().getName());
		}
		HandlerUtil.ipsConfigUpdate();
		TaskUtil.scheduleAtFixedRate(()->{ HandlerUtil.ipsCounterClear(null); }, DateUtil.parse(DateUtil.format(new Date(), "yyyy-MM-dd")+" 00:00:29"), 1, TimeUnit.DAYS);
	}

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();
		String service = queryParameters.remove("*").getFirst();
		log.info("{} {}", exchange.getRequestMethod(), exchange.getRequestURI());
		int dot = service.indexOf('.');
		String[] split = StringUtils.split(dot>0 ? service.substring(0, dot) : service, "/");
		if(split.length > 0) {
			String name = split[0];
			AbstractHandler handler = handlers.get(name);
			if(handler != null) {
				String path = split.length>1 ? split[1] : StringUtils.EMPTY;
				exchange.putAttachment(AbstractHandler.PATH, path);
				HandlerUtil.parseBody(exchange);
				boolean ipsConfig = HandlerUtil.ipsConfig(exchange, name);
				if(ipsConfig) {
					handler.handleRequest(exchange);
				}else {
					HandlerUtil.setResp(exchange, Collections.singletonMap("error", "access is limited"));
				}
				serviceCount(name);
			}
		}
		HandlerUtil.sendResp(exchange);
	}
	/** 支持子服务名反射调用同名方法，子服务存入RESP对象即可 */
	@Slf4j
	public static abstract class AbstractHandler implements LightHttpHandler {
		/**
		 * 子服务路径，存在时反射调用子服务方法，否则子类覆盖handleRequest方法
		 */
		public static final AttachmentKey<String> PATH = AttachmentKey.create(String.class);
		private Map<String, Method> methods = new HashMap<>();
		public AbstractHandler() {
			Method[] declaredMethods = getClass().getDeclaredMethods();
			String name = name();
			for(Method method : declaredMethods) {
				if("name".equals(method.getName())) {
					continue;
				}
				if(Modifier.isPublic(method.getModifiers())) {
					// 暂未检查returnType和parameterTypes
					methods.put(method.getName(), method);
					log.info("{}/{} => {}", name, method.getName(), method);
				}
			}
		}
		/**
		 * ValidateHandler => validate
		 * @return 服务名
		 */
		public String name() {
			String simpleName = getClass().getSimpleName();
			int handler = simpleName.indexOf("Handler");
			String name = handler>0 ? simpleName.substring(0, handler) : simpleName;
			return new StringBuilder(name.length()).append(Character.toLowerCase(name.charAt(0))).append(name.substring(1)).toString();
		}

		@Override
		public void handleRequest(HttpServerExchange exchange) throws Exception {
			String path = exchange.getAttachment(PATH);
			Method method = methods.get(StringUtil.isBlank(path) ? name() : path);
			if(method != null) {
				try {
					method.invoke(this, exchange);
					return;
				}catch(Exception e) {
					String msg = e.getMessage();
					if(e instanceof InvocationTargetException) {
						InvocationTargetException ex = (InvocationTargetException)e;
						Throwable t = ex.getTargetException();
						if(t != null) {
							msg = t.getMessage();
						}
					}
					log.warn("service handle failed, path: {}, ex: {}", path, msg);
				}
			}
		}
	}
	/**
	 * 支持所有http方法：/service/* => ServiceHandler
	 * @author xlongwei
	 *
	 */
	public static class ServiceEndpointSource extends PathEndpointSource {
		public ServiceEndpointSource() {
			super("/service/*");
		}
	}
	public static void serviceCount(boolean serviceCount) {
		ServiceHandler.serviceCount = serviceCount;
		LoggerContext lc = (LoggerContext)LoggerFactory.getILoggerFactory();
		lc.getLogger("root").setLevel(serviceCount ? Level.INFO : Level.OFF);
	}
	public static void serviceCount(String name) {
		if(serviceCount) {
			serviceCounter.count("service", name);
		}
	}
	/** 统计service调用次数，定时保存到redis */
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
				for(Entry<String, AtomicInteger> count : tokenCount.counts.entrySet()) {
					counts.put(count.getKey(), count.getValue().get());
				}
			}else {
				for(Entry<String, AtomicInteger> count : tokenCount.counts.entrySet()) {
					Integer value = counts.get(count.getKey());
					if(value == null) {
						counts.put(count.getKey(), count.getValue().get());
					}else {
						counts.put(count.getKey(), value+count.getValue().get());
					}
				}
			}
			String value = JSONObject.toJSONString(counts);
			log.debug("update service counter key: {} count: {}", key, value);
			RedisConfig.persist(key, value);
		}
	}
}
