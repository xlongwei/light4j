package com.xlongwei.light4j.util;

import java.util.LinkedList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

/**
 * redis分片缓存
 * @author xlongwei
 *
 */
@Slf4j
public class RedisCache {
	public static int DEFAULT_SECONDS = 604800;
	public static ShardedJedisPool SHARDED_JEDIS_POOL;
	private static List<JedisShardInfo> nodes = new LinkedList<>();
	static {
		String hostAndPorts = ConfigUtil.config("redis").get("cacheDbs");
		log.info("redis.cacheDbs={}", hostAndPorts);
		
		String[] array = hostAndPorts.split("[,;]");
		for(String hostAndPort : array) {
			//host:port:1.2.3.4.5-8:password:timeout:weight
			nodes.addAll(nodes(hostAndPort));
		}
		RedisUtil.poolConfig.setMinIdle(3);
		SHARDED_JEDIS_POOL = new ShardedJedisPool(RedisUtil.poolConfig, nodes);
		log.info("redis cache loaded");
		
		TaskUtil.addShutdownHook(new Runnable() {
			@Override
			public void run() {
				SHARDED_JEDIS_POOL.destroy();
				log.info("redis cache shutdown");
			}
		});
	}
	
	public interface ShardedJedisCallback<T> {
		/**
		 * 操作ShardedJedis
		 * @param shardedJedis
		 * @return
		 */
		T doInShardedJedis(ShardedJedis shardedJedis);
	}
	
	public static <T> T execute(ShardedJedisCallback<T> callback) {
		ShardedJedis shardedJedis = SHARDED_JEDIS_POOL.getResource();
		try {
			return callback.doInShardedJedis(shardedJedis);
		}catch(Exception e){
			log.warn("redis cache fail: {}", e.getMessage());
			return null;
		}finally {
			SHARDED_JEDIS_POOL.returnResource(shardedJedis);
		}
	}
	
	public static String get(final String cache, final String key) {
		return execute(new ShardedJedisCallback<String>() {
			@Override
			public String doInShardedJedis(ShardedJedis shardedJedis) {
				byte[] byteKey = RedisUtil.byteKey(cache, key);
				byte[] byteValue = shardedJedis.get(byteKey);
				return RedisUtil.stringValue(byteValue);
			}
		});
	}
	
	public static void set(String cache, String key, String value) {
		set(cache, key, value, DEFAULT_SECONDS);
	}
	
	public static void set(final String cache, final String key, final String value, final int seconds) {
		execute(new ShardedJedisCallback<String>() {
			@Override
			public String doInShardedJedis(ShardedJedis shardedJedis) {
				byte[] byteKey = RedisUtil.byteKey(cache, key);
				byte[] byteValue = RedisUtil.byteValue(value);
				shardedJedis.set(byteKey, byteValue);
				expire(shardedJedis, byteKey, seconds);
				return null;
			}
		});
	}
	
	/** 超时过期 */
	public static void expire(final String cache, final String key, final int seconds) {
		execute(new ShardedJedisCallback<String>() {
			@Override
			public String doInShardedJedis(ShardedJedis shardedJedis) {
				byte[] byteKey = RedisUtil.byteKey(cache, key);
				expire(shardedJedis, byteKey, seconds);
				return null;
			}
		});
	}
	
	/** 超时获取 */
	public static Long ttl(final String cache, final String key) {
		return execute(new ShardedJedisCallback<Long>() {
			@Override
			public Long doInShardedJedis(ShardedJedis shardedJedis) {
				byte[] byteKey = RedisUtil.byteKey(cache, key);
				return shardedJedis.ttl(byteKey);
			}
		});
	}
	
	/** 获取分片 */
	public static MyJedisShardInfo shardInfo(final String cache, final String key) {
		return execute(new ShardedJedisCallback<MyJedisShardInfo>() {
			@Override
			public MyJedisShardInfo doInShardedJedis(ShardedJedis shardedJedis) {
				byte[] byteKey = RedisUtil.byteKey(cache, key);
				return (MyJedisShardInfo)shardedJedis.getShardInfo(byteKey);
			}
		});
	}
	
	private static List<JedisShardInfo> nodes(String hostAndPort){
		String[] hostPort = hostAndPort.split("[:]");
		String host = hostPort[0];
		int port = NumberUtil.parseInt(hostPort.length>1 ? hostPort[1] : null, 6379);
		String dbs = hostPort.length>2 ? hostPort[2] : "0";
		String password = hostPort.length>3?hostPort[3]:null;
		int timeout = NumberUtil.parseInt(hostPort.length>4 ? hostPort[4] : null, 0);
		int weight = NumberUtil.parseInt(hostPort.length>5 ? hostPort[5] : null, 1);
		
		List<JedisShardInfo> nodes = new LinkedList<>();
		String dbSpliter = "[\\.]";
		for(String db : dbs.split(dbSpliter)) {
			int dash = db.indexOf('-');
			if(dash>0) {
				int from = Integer.parseInt(db.substring(0, dash)), to = Integer.parseInt(db.substring(dash+1));
				for(int i=from; i<=to; i++) {
					nodes.add(new MyJedisShardInfo(host, port, password, i, weight, timeout));
				}
			}else {
				nodes.add(new MyJedisShardInfo(host, port, password, Integer.parseInt(db), weight, timeout));
			}
		}
		return nodes;
	}
	
	private static void expire(ShardedJedis jedis, byte[] key, int seconds) {
		if(seconds > 0) {
			jedis.expire(key, seconds);
		}else {
			//cache without persist
		}
	}
	
	public static class MyJedisShardInfo extends JedisShardInfo {
		private int db;
		public MyJedisShardInfo(String host, int port, String password, int db, int weight, int timeout) {
			super(host, port, timeout, weight);
			this.db = db;
			setPassword(password);
		}
		public int getDb() { return db; }
		@Override
		public Jedis createResource() {
			Jedis jedis = super.createResource();
			jedis.connect();
			String password = getPassword();
			if(password!=null && password.length()>0) {
				jedis.auth(password);
			}
			if(db>0) {
				jedis.select(db);
			}
			return jedis;
		}
		@Override
		public String toString() {
			return new StringBuilder("MyJedisShardInfo(host=").append(getHost()).append(",port=").append(getPort())
					.append(",db=").append(getDb()).append(",password=").append(getPassword()).append(",timeout=")
					.append(getTimeout()).append(",weight=").append(getWeight()).append(")").toString();
		}
	}
}
