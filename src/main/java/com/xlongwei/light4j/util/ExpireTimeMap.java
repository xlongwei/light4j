package com.xlongwei.light4j.util;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 限时过期映射
 * @author hongwei
 */
@SuppressWarnings("unchecked")
public class ExpireTimeMap<K, V> extends AbstractMap<K, V> {
	private static Log log = LogFactory.getLog(ExpireTimeMap.class);
	
	private final Map<K, V> map;
	private long expireTime;
	private boolean afterLastAccess;
	private KeyExpireTimeHandler<K, V> keyExpireTimeHandler;
	private KeyNotFoundHandler<K, V> keyNotFoundHandler;
	private Map<K, Long> lastAccess = new HashMap<K, Long>();
	private Map<K, Long> lastWrite = new HashMap<K, Long>();
	/**
	 * 使用HashMap，最后写入之后expireTime有访问过时过期
	 */
	public ExpireTimeMap(long expireTime) {
		this(new HashMap<K, V>(), expireTime);
	}
	/**
	 * key=value最后写入之后expireTime过期（如果写入之后没有访问过也不过期）
	 */
	public ExpireTimeMap(Map<K, V> map, long expireTime){
		this(map, expireTime, false, null, null);
	}
	/**
	 * afterLastAccess=true时，key=value最后访问之后exireTime过期
	 */
	public ExpireTimeMap(Map<K, V> map, long expireTime, boolean afterLastAccess){
		this(map, expireTime, afterLastAccess, null, null);
	}
	public ExpireTimeMap(Map<K, V> map, long expireTime, boolean afterLastAccess, KeyExpireTimeHandler<K, V> keyExpireTimeHandler, KeyNotFoundHandler<K, V> keyNotFoundHandler){
		this.map = map;
		this.expireTime = expireTime;
		this.afterLastAccess = afterLastAccess;
		this.keyExpireTimeHandler = keyExpireTimeHandler;
		this.keyNotFoundHandler = keyNotFoundHandler;
		TaskUtil.scheduleAtFixedRate(new ExpireTimeChecker(), expireTime, expireTime, TimeUnit.MILLISECONDS);
	}
	public void setAfterLastAccess(boolean afterLastAccess) {
		this.afterLastAccess = afterLastAccess;
	}
	public void setKeyExpireTimeHandler(KeyExpireTimeHandler<K, V> keyExpireTimeHandler) {
		this.keyExpireTimeHandler = keyExpireTimeHandler;
	}
	public void setKeyNotFoundHandler(KeyNotFoundHandler<K, V> keyNotFoundHandler) {
		this.keyNotFoundHandler = keyNotFoundHandler;
	}
	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		return map.entrySet();
	}
	
	@Override
	public V get(Object key) {
		//试图获取值时锁定map，保证其它get(key)获得正确的值
		synchronized (map) {
			V value = map.get(key);
			
			if(value != null) {
				lastAccess.put((K)key, System.currentTimeMillis());
				log.info("last access ("+key+")="+lastAccess.get(key));
			}
			
			//找不到key=value时试图获取一个值
			if(value == null && keyNotFoundHandler != null) {
				value = keyNotFoundHandler.keyNotFound((K)key);
				if(value != null) {
					lastAccess.put((K)key, System.currentTimeMillis());
					log.info("last access ("+key+")="+lastAccess.get(key));
					put((K)key, value);
				}
			}
			return value;
		}
	}

	@Override
	public V put(K key, V value) {
		lastWrite.put(key, System.currentTimeMillis()+1);
		log.info("last write ("+key+")="+lastWrite.get(key));
		return map.put(key, value);
	}

	@Override
	public V remove(Object key) {
		V value = map.remove(key);
		
		if(value != null) {
			lastAccess.remove((K)key);
			lastWrite.remove((K)key);
		}
		return value;
	}
	
	private class ExpireTimeChecker implements Runnable {
		@Override
		public void run() {
			if(map.size() == 0) {
				return;
			}
			
			List<K> removeKeys = new LinkedList<K>();
			long currentTimeMillis = System.currentTimeMillis();
			synchronized (map) {
				for(K key : map.keySet()) {
					Long access = lastAccess.get(key);
					Long write = lastWrite.get(key);
					
					boolean expire = (afterLastAccess && access != null && currentTimeMillis - access >= expireTime) || 
							(!afterLastAccess && access != null && access >= write && currentTimeMillis - write >= expireTime);
					if(expire) {
						removeKeys.add(key);
					}
					
					if(log.isDebugEnabled()) {
						log.debug("("+key+") expire:"+expire+", access:"+access+", write:"+write+", minus:"+(access == null ? -1 : (afterLastAccess ? currentTimeMillis-access : access-write)));
					}
				}
				log.info("checked: " + map.keySet().toString());
			}
			
			if(removeKeys.size() == 0) {
				return;
			}
			for(K key : removeKeys) {
				//试图恢复值时锁定map让其他get(key)等待以获取正确的值
				synchronized (map) {
					V value = map.remove(key);
					
					//key=value已经过期了，试图恢复值
					if(value != null) {
						if(keyExpireTimeHandler != null) {
							value = keyExpireTimeHandler.keyExpireTime(key, value);
						}else if(keyNotFoundHandler != null) {
							value = keyNotFoundHandler.keyNotFound(key);
						}
						
						if(value != null) {
							put(key, value);
							continue;
						}else {
							log.info("("+key+") expired.");
						}
					}
					
					lastAccess.remove(key);
					lastWrite.remove(key);
				}
			}
		}
	}
	
	/**
	 * 键值对过期时可使用此接口恢复新值，返回value恢复原值，返回null清除键key，返回其它值更新键值key
	 */
	public static interface KeyExpireTimeHandler<K, V> {
		/**
		 * 键超时处理
		 * @param key
		 * @param value
		 * @return
		 */
		V keyExpireTime(K key, V value);
	}
	
	/**
	 * 找不到键值对时可使用此接口提供值
	 */
	public static interface KeyNotFoundHandler<K, V> {
		/**
		 * 键未见时处理
		 * @param key
		 * @return
		 */
		V keyNotFound(K key);
	}
}
