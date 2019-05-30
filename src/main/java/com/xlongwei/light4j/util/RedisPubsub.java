package com.xlongwei.light4j.util;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.xlongwei.light4j.util.RedisConfig.JedisCallback;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

/**
 * redis pubsub
 * @author xlongwei
 */
@Slf4j
public class RedisPubsub {
	public static final String CHANNEL = "pubsub";
	private static List<MessageListener> listeners = new CopyOnWriteArrayList<>();
	private static Map<String, JedisPubSub> pubsubs = new ConcurrentHashMap<>();
	
	/** 发布消息到默认渠道 */
	public static void pub(final String message) {
		pub(CHANNEL, message);
	}
	
	/** 发布消息到指定渠道 */
	public static void pub(final String channel, final String message) {
		RedisConfig.execute(new JedisCallback<String>() {
			@Override
			public String doInJedis(Jedis jedis) {
				jedis.publish(channel, message);
				return null;
			}
		});
	}
	
	/** 订阅默认渠道消息 */
	public static void sub(MessageListener listener) {
		listeners.add(listener);
	}
	
	/** 订阅指定渠道消息 */
	public static void sub(final String channel, final JedisPubSub pubsub) {
		if(StringUtil.isBlank(channel) || pubsub==null) {
			return;
		}
		TaskUtil.submitKeepRunning(new Runnable() {
			@Override
			public void run() {
				Jedis jedis = null;
				try {
					jedis = RedisConfig.JEDIS_POOL.getResource();
					jedis.subscribe(pubsub, channel);
				}finally {
					RedisConfig.JEDIS_POOL.returnBrokenResource(jedis);
				}
			}
		});
		pubsubs.put(channel, pubsub);
	}
	
	/**
	 * 默认渠道消息监听器
	 * @author xlongwei
	 */
	public static interface MessageListener {
		/**
		 * 接收消息通知
		 * @param message
		 */
		void onMessage(String message);
	}
	
	/** 消息监听器适配 */
	public static class JedisPubSubAdapter extends JedisPubSub {
		@Override
		public void onMessage(String channel, String message) {}
		@Override
		public void onPMessage(String pattern, String channel, String message) {}
		@Override
		public void onSubscribe(String channel, int subscribedChannels) {}
		@Override
		public void onUnsubscribe(String channel, int subscribedChannels) {}
		@Override
		public void onPUnsubscribe(String pattern, int subscribedChannels) {}
		@Override
		public void onPSubscribe(String pattern, int subscribedChannels) {}
	}
	
	static {
		//注册默认渠道监听器
		JedisPubSub pubsub = new JedisPubSubAdapter() {
			@Override
			public void onMessage(String channel, String message) {
				log.info("onMessage chanel: {}, message: {}", channel, message);
				for(MessageListener listener : listeners) {
					listener.onMessage(message);
				}
			}
		};
		sub(CHANNEL, pubsub);
		TaskUtil.addShutdownHook(new Runnable() {
			@Override
			public void run() {
				log.info("redis pubsub shutdown");
				for(Map.Entry<String, JedisPubSub> entry : pubsubs.entrySet()) {
					entry.getValue().unsubscribe(entry.getKey());
				}
			}
		});
	}
}
