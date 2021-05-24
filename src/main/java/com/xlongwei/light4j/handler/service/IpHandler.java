package com.xlongwei.light4j.handler.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.ConfigUtil;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.NumberUtil;
import com.xlongwei.light4j.util.RedisConfig;
import com.xlongwei.light4j.util.StringUtil;

import org.lionsoul.ip2region.DataBlock;
import org.lionsoul.ip2region.DbConfig;
import org.lionsoul.ip2region.DbSearcher;
import org.lionsoul.ip2region.Util;

import cn.hutool.core.map.MapUtil;
import io.undertow.server.HttpServerExchange;
import lombok.extern.slf4j.Slf4j;

/**
 * ip handler
 * @author xlongwei
 *
 */
@Slf4j
public class IpHandler extends AbstractHandler {
	static DbSearcher dbSearcher = null;
	static boolean memorySearch = NumberUtil.parseBoolean(RedisConfig.get("ip.memorySearch"), false);

	public void region(HttpServerExchange exchange) throws Exception {
		String ip = HandlerUtil.getParam(exchange, "ip");
		boolean addIp = false;
		if(StringUtil.isBlank(ip)) {
			ip = HandlerUtil.getParam(exchange, "showapi_userIp");
			if(StringUtil.isBlank(ip)) {
				ip = HandlerUtil.getIp(exchange);
			}
			addIp = true;
		}
		Map<String, String> map = searchToMap(ip);
		if(MapUtil.isNotEmpty(map)) {
			if(addIp) {
				map.put("ip", ip);
			}
			HandlerUtil.setResp(exchange, map);
		}
	}

	public void lock(HttpServerExchange exchange) throws Exception {
		String key = HandlerUtil.getParam(exchange, "key");
		if (StringUtil.isBlank(key)) {
			return;
		}
		String locker = HandlerUtil.getParam(exchange, "locker");
		int seconds = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "seconds"), 10);
		boolean isShowapiRequest = HandlerUtil.isShowapiRequest(exchange);
		String userName = "common";
		if (isShowapiRequest) {
			String showapiUserName = HandlerUtil.getShowapiUserName(exchange);
			if (!StringUtil.isBlank(showapiUserName)) {
				userName = showapiUserName;
			}
			if (StringUtil.isBlank(locker)) {
				locker = HandlerUtil.getParam(exchange, "showapi_userIp");
			}
		}
		if (StringUtil.isBlank(locker)) {
			locker = HandlerUtil.getIp(exchange);
		}
		final String redisKey = RedisConfig.CACHE + ":lock." + userName + "." + key;
		final String redisValue = locker;
		final int redisSeconds = seconds >= 6 && seconds <= 600 ? seconds : 60;
		Map<String, Object> map = new HashMap<>();
		RedisConfig.execute(jedis -> {
			Long setnx = jedis.setnx(redisKey, redisValue);
			if (setnx != null && setnx.longValue() == 1L) {
				jedis.expire(redisKey, redisSeconds);
			}
			map.put("locker", jedis.get(redisKey));
			map.put("ttl", jedis.ttl(redisKey));
			return null;
		});
		map.put("lock", locker.equals(map.get("locker")));
		HandlerUtil.setResp(exchange, map);
	}

	public void config(HttpServerExchange exchange) throws Exception {
		String memory = HandlerUtil.getParam(exchange, "memory");
		Boolean search = "true".equals(memory) ? Boolean.TRUE : ("false".equals(memory) ? Boolean.FALSE : null);
		if(search!=null && search.booleanValue()!=memorySearch) {
			dbSearcher.close();
			reload();
			memorySearch = search.booleanValue();
		}
		HandlerUtil.setResp(exchange, Collections.singletonMap("memory", memorySearch));
	}
	
	private static String zeroToEmpty(String value) {
		return "0".equals(value) ? "" : value;
	}
	
	static {
		reload();
	}

	private static void reload() {
		try {
			DbConfig config = new DbConfig();
			String dbFile = ConfigUtil.DIRECTORY + "ip2region.db";
            dbSearcher = new DbSearcher(config, dbFile);
		}catch(Exception e) {
			log.warn("fail to init DbSearcher: {}", e.getMessage());
		}
	}
	
	public static DataBlock search(String ip) {
		if(StringUtil.hasLength(ip)) {
//			if("0:0:0:0:0:0:0:1".equals(ip)) {
//				ip = "127.0.0.1";
//			}
			if(Util.isIpAddress(ip)) {
				synchronized (dbSearcher) {
					try{
						if(memorySearch) {
							return dbSearcher.memorySearch(ip);
						}
						return dbSearcher.btreeSearch(ip);
					}catch(Exception e) {
						log.warn("fail to search ip: {}, ex: {}", ip, e.getMessage());
					}
				}
			}
		}
		return null;
	}
	
	public static Map<String, String> searchToMap(String ip) {
		DataBlock dataBlock = search(ip);
		if(dataBlock != null) {
			//国家，区域，省份，城市，运营商
			String region = dataBlock.getRegion();
			if(StringUtil.hasLength(region)) {
				Map<String, String> map = new LinkedHashMap<>(8);
				String[] split = region.split("[|]", 5);
				int idx = 0;
				map.put("country", zeroToEmpty(split[idx++]));
				map.put("area", zeroToEmpty(split[idx++]));
				map.put("state", zeroToEmpty(split[idx++]));
				map.put("city", zeroToEmpty(split[idx++]));
				map.put("isp", zeroToEmpty(split[idx++]));
				map.put("region", StringUtil.join(map.values(), null, null, null));
				return map;
			}
		}
		return Collections.emptyMap();
	}
}
