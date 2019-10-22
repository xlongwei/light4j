package com.xlongwei.light4j.handler.service;

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

import io.undertow.server.HttpServerExchange;
import lombok.extern.slf4j.Slf4j;

/**
 * ip handler
 * @author xlongwei
 *
 */
@Slf4j
public class IpHandler extends AbstractHandler {
	DbSearcher dbSearcher = null;

	public void region(HttpServerExchange exchange) throws Exception {
		String ip = HandlerUtil.getParam(exchange, "ip");
		if(StringUtil.hasLength(ip) && Util.isIpAddress(ip)) {
			initDbSearcher();
			DataBlock dataBlock = dbSearcher.memorySearch(ip);
			Map<String, String> map = new LinkedHashMap<>(4);
			if(dataBlock != null) {
				//国家，区域，省份，城市，运营商
				String region = dataBlock.getRegion();
				if(StringUtil.hasLength(region)) {
					String[] split = region.split("[|]", 5);
					int idx = 0;
					map.put("country", trim(split[idx++]));
					map.put("area", trim(split[idx++]));
					map.put("state", trim(split[idx++]));
					map.put("city", trim(split[idx++]));
					map.put("isp", trim(split[idx++]));
					map.put("region", StringUtil.join(map.values(), null, null, null));
				}
			}
			HandlerUtil.setResp(exchange, map);
		}
	}
	
	private String trim(String value) {
		return "0".equals(value) ? "" : value;
	}
	
	private void initDbSearcher() {
		if(dbSearcher == null) {
			try {
				DbConfig config = new DbConfig();
				String dbFile = ConfigUtil.DIRECTORY + "ip2region.db";
	            dbSearcher = new DbSearcher(config, dbFile);
			}catch(Exception e) {
				log.warn("fail to init DbSearcher: {}", e.getMessage());
			}
		}
	}
}
