package com.networknt.config;

import java.lang.reflect.Method;
import java.util.Map;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

/**
 * 相比Config.getJsonObjectConfig、getJsonMapConfig增加一层代理，获取配置时直接从缓存取值，以便刷新配置后可以实时更新相应配置
 * @author xlongwei
 *
 */
public class RefreshScope {

	public static <T> T getJsonObjectConfig(String configName, Class<T> clazz) {
		return create(new JsonObjectConfigCallback(configName, clazz, ""));
	}
	
	public static <T> T getJsonObjectConfig(String configName, Class<T> clazz, String path) {
		return create(new JsonObjectConfigCallback(configName, clazz, path));
	}
	
	public static Map<String, Object> getJsonMapConfig(String configName) {
		return create(new JsonMapConfigCallback(configName, Map.class, ""));
	}
	
	public static Map<String, Object> getJsonMapConfig(String configName, String path) {
		return create(new JsonMapConfigCallback(configName, Map.class, path));
	}
	
	@SuppressWarnings("unchecked")
	static <T> T create(ConfigCallback cc) {
		final Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(cc.clazz);
		enhancer.setCallback(cc);
		return (T) enhancer.create();
	}
	
	static abstract class ConfigCallback implements MethodInterceptor {
		String configName, path;
		Class<?> clazz;
		abstract Object getConfig();
		ConfigCallback(String configName, Class<?> clazz, String path){
			this.configName = configName;
			this.clazz = clazz;
			this.path = path;
		}
		@Override
		public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
			Object config = getConfig();
			return proxy.invoke(config, args);
		}
	}
	
	static class JsonObjectConfigCallback extends ConfigCallback {
		public JsonObjectConfigCallback(String configName, Class<?> clazz, String path) {
			super(configName, clazz, path);
		}
		@Override
		Object getConfig() {
			return Config.getInstance().getJsonObjectConfig(configName, clazz, path);
		}
	}
	
	static class JsonMapConfigCallback extends ConfigCallback {
		public JsonMapConfigCallback(String configName, Class<?> clazz, String path) {
			super(configName, clazz, path);
		}
		@Override
		Object getConfig() {
			return Config.getInstance().getJsonMapConfig(configName, path);
		}
	}
}
