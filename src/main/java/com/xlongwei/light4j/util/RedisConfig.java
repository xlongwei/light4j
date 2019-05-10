package com.xlongwei.light4j.util;

/**
 * config using redis
 * @author xlongwei
 *
 */
public class RedisConfig {
	public static final String CACHE = "property";
	
	public static String get(String key) {
		return RedisUtil.get(CACHE, key);
	}
	
	public static void set(String key, String value) {
		RedisUtil.set(CACHE, key, value);
	}
	
	public static void delete(String key) {
		RedisUtil.delete(CACHE, key);
	}
	
	public static void persist(String key, String value) {
		RedisUtil.persist(CACHE, key, value);
	}
}
