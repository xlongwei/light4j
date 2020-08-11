package com.xlongwei.light4j.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.commons.codec.binary.StringUtils;

import redis.clients.jedis.JedisPoolConfig;

/**
 * redis公共部分
 * @author xlongwei
 *
 */
public class RedisUtil {
	
	static JedisPoolConfig poolConfig = new JedisPoolConfig();
	
	static {
		poolConfig.setMinIdle(Integer.getInteger("redis.minIdle", 1));
		//<=0时，不启动Timer-0线程，容易出现SocketException，因此RedisCache、RedisConfig默认重试一次
		poolConfig.setTimeBetweenEvictionRunsMillis(Integer.getInteger("redis.timeBetweenEvictionRunsMillis", 0));
	}

	/** 获取字节key */
	public static byte[] byteKey(String key) {
		return StringUtils.getBytesUtf8(key);
	}
	
	/** 默认使用冒号分隔 */
	public static byte[] byteKey(String cache, String key) {
		return StringUtils.getBytesUtf8(cache+":"+key);
	}
	
	/** 获取String key */
	public static String stringKey(byte[] byteKey) {
		return StringUtils.newStringUtf8(byteKey);
	}

	/** 默认使用jdk序列化对象 */
	public static byte[] byteValue(String value) {
		if(value == null) {
			return new byte[0];
		}
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

}
