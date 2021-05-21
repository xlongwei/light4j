package com.xlongwei.light4j.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.time.DateFormatUtils;

import com.xlongwei.light4j.util.IdWorker.SystemClock;

import lombok.extern.slf4j.Slf4j;

/**
 * 继承TokenCounter，实现getId、update、insert<br/>
 * 配置getExpireCount、getExpireTime、getDate<br/>
 * 初始化new MyTokenCounter().init()<br/>
 * 统计MyTokenCounter.count(token,type)
 * @author hongwei
 */
@Slf4j
public class TokenCounter {
	public int expireCount = 20;
	public long expireTime = TimeUnit.SECONDS.toMillis(60);
	private String date = getDate();
	private final BlockingQueue<String> removeTokens = new LinkedBlockingQueue<String>();
	private final BlockingQueue<TokenType> tokenTypes = new LinkedBlockingQueue<TokenType>();
	private final Map<String, TokenCount> tokenCounts = new HashMap<String, TokenCount>();
	private final Map<String, Integer> idCaches = new ExpireTimeMap<String, Integer>(new HashMap<String, Integer>(), TimeUnit.MINUTES.toMillis(3));
	
	public void init(){
		TaskUtil.submitKeepRunning(tokenTypesConsumer);
		TaskUtil.submitKeepRunning(removeKeysConsumer);
		TaskUtil.scheduleAtFixedRate(expireTimeChecker, expireTime, expireTime, TimeUnit.MILLISECONDS);
	}
	
	public void count(String token, String type) {
		tokenTypes.add(new TokenType(token, type));
	}
	
	protected Integer getId(TokenCount tokenCount) { return tokenCount.expireTime() ? 1 : null; }
	protected void update(TokenCount tokenCount, Integer id) { log.debug("update " + id + ": " + tokenCount.toString()); }
	protected void insert(TokenCount tokenCount) { log.debug("insert: " + tokenCount.toString()); }
	
	protected String getDate() { return DateFormatUtils.format(SystemClock.now(), "yyyy-MM-dd"); }
	
	protected void merge(TokenCount tokenCount) {
		String cacheKey = tokenCount.token+tokenCount.day;
		Integer cacheId = idCaches.get(cacheKey);
		if(cacheId == null) {
			cacheId = getId(tokenCount);
			if(cacheId != null) {
				idCaches.put(cacheKey, cacheId);
			}
		}
		
		if(cacheId != null) {
			update(tokenCount, cacheId);
		}else {
			insert(tokenCount);
		}
	}
	
	public class TokenCount {
		public long time;
		public String token;
		public String day;
		public Map<String, AtomicInteger> counts = new HashMap<String, AtomicInteger>();
		private TokenCount(long time, String token, String date, String type) {
			this.time = time;
			this.token = token;
			this.day = date;
			this.counts.put(type, new AtomicInteger(1));
		}
		public boolean expireTime() {
			return SystemClock.now() - time >= expireTime;
		}
		public boolean expireCount() {
			int sum = 0;
			for(AtomicInteger count : counts.values()) {
				sum += count.get();
			}
			return sum >= expireCount;
		}
		public void count(String type) {
			AtomicInteger count = counts.get(type);
			if(count == null) {
				counts.put(type, new AtomicInteger(1));
			}else {
				count.incrementAndGet();
			}
		}
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(DateFormatUtils.format(time, "yyyy-MM-dd HH:mm:ss")).append(" ").append(token).append(" (");
			for(Entry<String, AtomicInteger> count : counts.entrySet()) {
				sb.append(count.getKey()).append("=").append(count.getValue().get()).append(",");
			}
			sb.setCharAt(sb.length()-1, ')');
			return sb.toString();
		}
	}
	
	static class TokenType {
		public String token;
		public String type;
		public TokenType(String token, String type) {
			this.token = token;
			this.type = type;
		}
	}
	
	final Runnable tokenTypesConsumer = () -> {
			while(true) {
				try {
					TokenType tokenType = tokenTypes.take();
					synchronized (tokenCounts) {
						TokenCount tokenCount = tokenCounts.get(tokenType.token);
						if(tokenCount == null) {
							tokenCount = new TokenCount(SystemClock.now(), tokenType.token, date, tokenType.type);
							tokenCounts.put(tokenType.token, tokenCount);
						}else {
							tokenCount.count(tokenType.type);
						}
						if(tokenCount.expireCount()) {
							log.debug("expire count: " + tokenCount.toString());
							removeTokens.add(tokenType.token);
						}
					}
				} catch (InterruptedException e) {
					log.error("tokenTypesConsumer interrupted");
					break;
				}
			}
	};
	
	final Runnable removeKeysConsumer = () -> {
			while(true) {
				try {
					String key = removeTokens.take();
					TokenCount tokenCount = null;
					synchronized (tokenCounts) {
						tokenCount = tokenCounts.remove(key);
					}
					if(tokenCount != null) {
						log.debug("merge removed: " + tokenCount.toString());
						merge(tokenCount);
					}
				} catch (InterruptedException e) {
					log.error("removeKeysConsumer interrupted");
					break;
				}
			}
	};
	
	final Runnable expireTimeChecker = () -> {
			synchronized (tokenCounts) {
				for(TokenCount tokenCount : tokenCounts.values()) {
					if(tokenCount.expireTime()) {
						log.debug("expire time: " + tokenCount.toString());
						removeTokens.add(tokenCount.token);
					}
				}
			}
			date = getDate();
	};
}
