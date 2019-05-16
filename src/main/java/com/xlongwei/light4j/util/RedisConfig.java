package com.xlongwei.light4j.util;

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
	public static final String CACHE = "property";
	public static final JedisPool JEDIS_POOL;
	
	static {
		String hostAndPort = ConfigUtil.config("redis").get("configDb");
		log.info("redis.configDb={}", hostAndPort);
		
		String[] hostPort = hostAndPort.split("[:]");
		String host = hostPort[0];
		int port = NumberUtil.parseInt(hostPort.length>1 ? hostPort[1] : null, 6379);
		int db = NumberUtil.parseInt(hostPort.length>2 ? hostPort[2] : null, 0);
		String password = hostPort.length>3?hostPort[3]:null;
		int timeout = NumberUtil.parseInt(hostPort.length>4 ? hostPort[4] : null, 0);
		JEDIS_POOL = new JedisPool(RedisUtil.poolConfig, host, port, timeout, password, db);
		log.info("redis config loaded");
		
		TaskUtil.addShutdownHook(new Runnable() {
			@Override
			public void run() {
				JEDIS_POOL.destroy();
				log.info("redis config shutdown");
			}
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
	
	public interface JedisCallback<T> {
		/**
		 * 操作Jedis，必要时返回对象
		 * @param jedis
		 * @return
		 */
		T doInJedis(Jedis jedis);
	}
	
	public static <T> T execute(JedisCallback<T> callback) {
		Jedis jedis = JEDIS_POOL.getResource();
		try {
			return callback.doInJedis(jedis);
		}catch(Exception e){
			log.warn("redis config fail: {}", e.getMessage());
			return null;
		}finally {
			JEDIS_POOL.returnResource(jedis);
		}
	}
	
	public static String get(final String cache, final String key) {
		return execute(new JedisCallback<String>() {
			@Override
			public String doInJedis(Jedis jedis) {
				byte[] byteKey = RedisUtil.byteKey(cache, key);
				byte[] byteValue = jedis.get(byteKey);
				return RedisUtil.stringValue(byteValue);
			}
		});
	}
	
	public static void set(String cache, String key, String value) {
		set(cache, key, value, 604800);
	}
	
	public static void set(final String cache, final String key, final String value, final int seconds) {
		execute(new JedisCallback<String>() {
			@Override
			public String doInJedis(Jedis jedis) {
				byte[] byteKey = RedisUtil.byteKey(cache, key);
				byte[] byteValue = RedisUtil.byteValue(value);
				jedis.set(byteKey, byteValue);
				expire(jedis, byteKey, seconds);
				return null;
			}
		});
	}
	
	public static void persist(String cache, String key, String value) {
		set(cache, key, value, -1);
	}
	
	public static void expire(final String cache, final String key, final int expire) {
		execute(new JedisCallback<String>() {
			@Override
			public String doInJedis(Jedis jedis) {
				byte[] byteKey = RedisUtil.byteKey(cache, key);
				expire(jedis, byteKey, expire);
				return null;
			}
		});
	}
	
	public static Long ttl(final String cache, final String key) {
		return execute(new JedisCallback<Long>() {
			@Override
			public Long doInJedis(Jedis jedis) {
				byte[] byteKey = RedisUtil.byteKey(cache, key);
				return jedis.ttl(byteKey);
			}
		});
	}
	
	public static void delete(final String cache, final String key) {
		execute(new JedisCallback<Long>() {
			@Override
			public Long doInJedis(Jedis jedis) {
				byte[] byteKey = RedisUtil.byteKey(cache, key);
				jedis.del(byteKey);
				return null;
			}
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
