package com.xlongwei.light4j.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

import org.apache.commons.codec.binary.StringUtils;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Slf4j
public class RedisUtil {
	public static final JedisPool jedisPool;
	
	static {
		Map<String, String> config = ConfigUtil.redis();
		JedisPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.setMaxIdle(30);
		poolConfig.setMaxWait(3000L);
		String hostAndPort = config.get("configDb");
		log.info("redis.configDb={}", hostAndPort);
		
		String[] hostPort = hostAndPort.split("[:]");
		String host = hostPort[0];
		int port = NumberUtil.parseInt(hostPort.length>1 ? hostPort[1] : null, 6379);
		int db = NumberUtil.parseInt(hostPort.length>2 ? hostPort[2] : null, 0);
		String password = hostPort.length>3?hostPort[3]:null;
		int timeout = NumberUtil.parseInt(hostPort.length>4 ? hostPort[4] : null, 0);
		jedisPool = new JedisPool(poolConfig, host, port, timeout, password, db);
		
		TaskUtil.addShutdownHook(new Runnable() {
			@Override
			public void run() {
				jedisPool.destroy();
			}
		});
	}
	
	public interface JedisCallback<T> {
		T doInJedis(Jedis jedis);
	}
	
	public static <T> T execute(JedisCallback<T> callback) {
		Jedis jedis = jedisPool.getResource();
		try {
			return callback.doInJedis(jedis);
		}finally {
			jedisPool.returnResource(jedis);
		}
	}
	
	public static String get(final String cache, final String key) {
		return execute(new JedisCallback<String>() {
			@Override
			public String doInJedis(Jedis jedis) {
				byte[] byteKey = byteKey(cache, key);
				byte[] byteValue = jedis.get(byteKey);
				return stringValue(byteValue);
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
				byte[] byteKey = byteKey(cache, key);
				byte[] byteValue = byteValue(value);
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
				byte[] byteKey = byteKey(cache, key);
				expire(jedis, byteKey, expire);
				return null;
			}
		});
	}
	
	public static Long ttl(final String cache, final String key) {
		return execute(new JedisCallback<Long>() {
			@Override
			public Long doInJedis(Jedis jedis) {
				byte[] byteKey = byteKey(cache, key);
				return jedis.ttl(byteKey);
			}
		});
	}
	
	public static void delete(final String cache, final String key) {
		execute(new JedisCallback<Long>() {
			@Override
			public Long doInJedis(Jedis jedis) {
				byte[] byteKey = byteKey(cache, key);
				jedis.del(byteKey);
				return null;
			}
		});
	}
	
	/** 默认使用冒号分隔 */
	public static byte[] byteKey(String cache, String key) {
		return StringUtils.getBytesUtf8(cache+":"+key);
	}
	
	/** 默认使用jdk序列化对象 */
	public static byte[] byteValue(String value) {
		if(value == null) return new byte[0];
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(value);
			oos.flush();
			return baos.toByteArray();
		}catch(Exception e) {
			return null;
		}
	}
	
	/** 默认使用jdk序列化对象 */
	public static String stringValue(byte[] byteValue) {
		try {
			ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(byteValue));
			return ois.readObject().toString();
		}catch(Exception e) {
			return null;
		}
	}
	
	private static void expire(Jedis jedis, byte[] key, int seconds) {
		if(seconds > 0) {
			jedis.expire(key, seconds);
		}else {
			jedis.persist(key);
		}
	}
}
