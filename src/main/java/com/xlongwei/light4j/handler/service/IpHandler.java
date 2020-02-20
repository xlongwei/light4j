package com.xlongwei.light4j.handler.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.lionsoul.ip2region.DataBlock;
import org.lionsoul.ip2region.DbConfig;
import org.lionsoul.ip2region.DbSearcher;
import org.lionsoul.ip2region.Util;

import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.ConfigUtil;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.StringUtil;

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
	
	public static String zeroToEmpty(String value) {
		return "0".equals(value) ? "" : value;
	}
	
	static {
		try {
			DbConfig config = new DbConfig();
			String dbFile = ConfigUtil.DIRECTORY + "ip2region.db";
            dbSearcher = new DbSearcher(config, dbFile);
		}catch(Exception e) {
			log.warn("fail to init DbSearcher: {}", e.getMessage());
		}
	}
	
	public static synchronized DataBlock search(String ip) {
		if(StringUtil.hasLength(ip) && Util.isIpAddress(ip)) {
			try{
				return dbSearcher.memorySearch(ip);
			}catch(Exception e) {
				log.warn("fail to search ip: {}, ex: {}", ip, e.getMessage());
			}
		}
		return null;
	}
	
	public static synchronized Map<String, String> searchToMap(String ip) {
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
