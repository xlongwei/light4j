package com.xlongwei.light4j.handler.demo;

import java.util.HashMap;
import java.util.Set;

import org.beetl.sql.core.db.MetadataManager;

import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.handler.service.IpHandler;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.MySqlUtil;
import com.xlongwei.light4j.util.StringUtil;

import cn.hutool.core.map.MapUtil;
import io.undertow.server.HttpServerExchange;

public class IndexHandler extends AbstractHandler {

	public void index(HttpServerExchange exchange) throws Exception {
		String ip = HandlerUtil.getIp(exchange);
		String region = IpHandler.searchToMap(ip).get("region");
		HandlerUtil.setResp(exchange, StringUtil.params("ip", ip, "region", region));
	}
	
	public void mysql(HttpServerExchange exchange) throws Exception {
		MetadataManager metaDataManager = MySqlUtil.SQLMANAGER.getMetaDataManager();
		Set<String> allTable = metaDataManager.allTable();
		HashMap<String, Object> map = MapUtil.newHashMap();
		map.put("allTable", allTable);
		HandlerUtil.setResp(exchange, map);
	}

}
