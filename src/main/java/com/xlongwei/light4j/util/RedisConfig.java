package com.xlongwei.light4j.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Supplier;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * redis单库配置
 * @author xlongwei
 *
 */
@Slf4j
public class RedisConfig {
	public static final int DEFAULT_SECONDS = 604800;
	public static final String CACHE = "property";
	public static final String LOCK = "lock";
	public static final JedisPool JEDIS_POOL;
	public static String host = "localhost";
	public static int port = 6379;
	
	static {
		String hostAndPort = ConfigUtil.config("redis").get("configDb");
		log.info("redis.configDb={}", hostAndPort);
		
		String[] hostPort = hostAndPort.split("[:]");
		host = hostPort[0];
		port = NumberUtil.parseInt(hostPort.length>1 ? hostPort[1] : null, 6379);
		int db = NumberUtil.parseInt(hostPort.length>2 ? hostPort[2] : null, 0);
		String password = hostPort.length>3?hostPort[3]:null;
		int timeout = NumberUtil.parseInt(hostPort.length>4 ? hostPort[4] : null, 0);
		JEDIS_POOL = new JedisPool(RedisUtil.poolConfig, host, port, timeout, password, db);
		log.info("redis config loaded");
		
		TaskUtil.addShutdownHook((Runnable)() -> {
				JEDIS_POOL.destroy();
				log.info("redis config shutdown");
		});
	}
	
	public static String get(String key) {
		return RedisConfig.get(CACHE, key);
	}
	
	public static void set(String key, String value) {
		RedisConfig.set(CACHE, key, value);
	}
	
	public static void delete(String key) {
		RedisConfig.delete(CACHE, key);
	}
	
	public static void persist(String key, String value) {
		RedisConfig.persist(CACHE, key, value);
	}
	
	@FunctionalInterface
	public interface JedisCallback<T> {
		/**
		 * 操作Jedis，必要时返回对象
		 * @param jedis
		 * @return
		 */
		T doInJedis(Jedis jedis);
	}
	
	public static <T> T execute(JedisCallback<T> callback) {
		return execute(callback, 1);
	}
	
	public static <T> T execute(JedisCallback<T> callback, int retries) {
		Jedis jedis = JEDIS_POOL.getResource();
		T t = null;
		try {
			t = callback.doInJedis(jedis);
			JEDIS_POOL.returnResource(jedis);
		}catch(Exception e){
			log.warn("redis config fail: {}", e.getMessage());
			JEDIS_POOL.returnBrokenResource(jedis);
			if(retries > 0) {
				t = execute(callback, retries - 1);
			}
		}
		return t;
	}
	
	public static String get(final String cache, final String key) {
		return execute((jedis) -> {
				byte[] byteKey = RedisUtil.byteKey(cache, key);
				byte[] byteValue = jedis.get(byteKey);
				return RedisUtil.stringValue(byteValue);
		});
	}
	
	/**
	 * 批量获取多个键值
	 * @param cache
	 * @param pattern exam.java1.*
	 * @return Map&lt;String, String>={key:value}，key去掉了cache前缀
	 */
	public static Map<String, String> gets(final String cache, final String pattern) {
		return execute((jedis) -> {
				byte[] byteKey = RedisUtil.byteKey(cache, pattern);
				Set<byte[]> keys = jedis.keys(byteKey);
				Map<String, String> map = new HashMap<>(16);
				int prefix = cache.length()+1;
				for(byte[] key:keys) {
					byte[] byteValue = jedis.get(key);
					String stringValue = RedisUtil.stringValue(byteValue);
					String stringKey = RedisUtil.stringKey(key);
					stringKey = stringKey.substring(prefix);
					map.put(stringKey, stringValue);
				}
				return map;
		});
	}
	
	public static String hget(final String cache, final String key, final String field) {
		return execute((jedis) -> {
				byte[] byteKey = RedisUtil.byteKey(cache, key);
				byte[] byteField = RedisUtil.byteKey(field);
				byte[] byteValue = jedis.hget(byteKey, byteField);
				return RedisUtil.stringValue(byteValue);
		});
	}
	
	public static Map<String, String> hgetAll(final String cache, final String key) {
		return execute((jedis) -> {
				byte[] byteKey = RedisUtil.byteKey(cache, key);
				Map<byte[], byte[]> hgetAll = jedis.hgetAll(byteKey);
				Map<String, String> hgetMap = new HashMap<>(16);
				for(Entry<byte[], byte[]> entry : hgetAll.entrySet()) {
					byte[] field = entry.getKey();
					byte[] value = entry.getValue();
					hgetMap.put(RedisUtil.stringKey(field), RedisUtil.stringValue(value));
				}
				return hgetMap;
		});
	}
	
	/**
	 * 获取缓存值，不存在时从supplier取值，并存入缓存
	 * @param cache
	 * @param key
	 * @param supplier 延迟计算，缓存有值时不会计算求值
	 * @return
	 */
	public static String get(final String cache, final String key, final Supplier<String> supplier) {
		return execute((jedis) -> {
				byte[] byteKey = RedisUtil.byteKey(cache, key);
				byte[] byteValue = jedis.get(byteKey);
				String value = RedisUtil.stringValue(byteValue);
				if(StringUtil.isBlank(value) && supplier!=null) {
					value = supplier.get();
					if(StringUtil.hasLength(value)) {
						byteValue = RedisUtil.byteValue(value);
						jedis.set(byteKey, byteValue);
					}
				}
				return value;
		});
	}
	
	public static void set(String cache, String key, String value) {
		set(cache, key, value, DEFAULT_SECONDS);
	}
	
	public static void set(final String cache, final String key, final String value, final int seconds) {
		execute((jedis) -> {
				byte[] byteKey = RedisUtil.byteKey(cache, key);
				byte[] byteValue = RedisUtil.byteValue(value);
				jedis.set(byteKey, byteValue);
				expire(jedis, byteKey, seconds);
				return null;
		});
	}
	
	public static void hset(final String cache, final String key, final String field, final String value) {
		execute((jedis) -> {
				byte[] byteKey = RedisUtil.byteKey(cache, key);
				byte[] byteField = RedisUtil.byteKey(field);
				if(value == null) {
					jedis.hdel(byteKey, byteField);
				}else {
					byte[] byteValue = RedisUtil.byteValue(value);
					jedis.hset(byteKey, byteField, byteValue);
				}
				return Boolean.TRUE;
		});
	}
	
	public static void persist(String cache, String key, String value) {
		set(cache, key, value, -1);
	}
	
	public static void expire(final String cache, final String key, final int expire) {
		execute((jedis) -> {
				byte[] byteKey = RedisUtil.byteKey(cache, key);
				expire(jedis, byteKey, expire);
				return null;
		});
	}
	
	public static Long ttl(final String cache, final String key) {
		return execute((jedis) -> {
				byte[] byteKey = RedisUtil.byteKey(cache, key);
				return jedis.ttl(byteKey);
		});
	}
	
	public static void delete(final String cache, final String key) {
		execute((jedis) -> {
				byte[] byteKey = RedisUtil.byteKey(cache, key);
				jedis.del(byteKey);
				return null;
		});
	}
	
	/** 加入列表头部 */
	public static Long lpush(final String cache, final String key, final String value) {
		return execute((jedis) -> {
				byte[] byteKey = RedisUtil.byteKey(cache, key);
				return jedis.lpush(byteKey, RedisUtil.byteValue(value));
		});
	}
	
	/** 获取列表范围 */
	public static List<String> lrange(final String cache, final String key, final int start, final int end) {
		return execute((jedis) -> {
				byte[] byteKey = RedisUtil.byteKey(cache, key);
				List<byte[]> lrange = jedis.lrange(byteKey, start, end);
				List<String> list = new ArrayList<>(lrange.size());
				for(byte[] bs : lrange) {
					list.add(RedisUtil.stringValue(bs));
				}
				return list;
		});
	}
	
	/** 加入列表头部 */
	public static String ltrim(final String cache, final String key, final int start, final int end) {
		return execute((jedis) -> {
				byte[] byteKey = RedisUtil.byteKey(cache, key);
				return jedis.ltrim(byteKey, start, end);
		});
	}
	
	/** 分布式锁，无限等待锁
	 * @param seconds 10 获得锁后锁定10秒
	 * */
	public static boolean lock(String cache, String key, int seconds) {
		return lock(cache, key, seconds, 0);
	}
	
	/** 分布式锁，限时等待锁
	 * @param seconds 10 获得锁后锁定10秒
	 * @param wait <=0 无限等待 10等待10秒
	 * */
	public static boolean lock(String cache, String key, final int seconds, final int wait) {
		final byte[] fk = RedisUtil.byteKey(cache, key);
		return execute((jedis) -> {
				long setnx = jedis.setnx(fk, fk);
				boolean locked = setnx==1;
				long totalWait = 0, tw = 31, fw = wait*1000L;
				while(!locked) {
					TaskUtil.sleep(tw);
					setnx = jedis.setnx(fk, fk);
					locked = setnx==1;
					totalWait += tw;
					if(fw>0 && totalWait>fw) {
						if(!locked) {
							return false;
						} else {
							break;
						}
					}
				}
				expire(jedis, fk, seconds);
				return true;
		});
	}
	
	private static void expire(Jedis jedis, byte[] key, int seconds) {
		if(seconds > 0) {
			jedis.expire(key, seconds);
		}else {
			jedis.persist(key);
		}
	}

}
