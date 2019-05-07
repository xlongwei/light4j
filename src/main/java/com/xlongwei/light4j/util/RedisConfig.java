package com.xlongwei.light4j.util;

public class RedisConfig {
	public static final String cache = "property";
	
	public static String get(String key) {
		return RedisUtil.get(cache, key);
	}
	
	public static void set(String key, String value) {
		RedisUtil.set(cache, key, value);
	}
	
	public static void delete(String key) {
		RedisUtil.delete(cache, key);
	}
	
	public static void persist(String key, String value) {
		RedisUtil.persist(cache, key, value);
	}
}
