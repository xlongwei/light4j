package com.xlongwei.light4j.handler.service;

import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.collections4.CollectionUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.StringUtil;
import com.xlongwei.light4j.util.TrainUtil;

import io.undertow.server.HttpServerExchange;

/**
 * train handler
 * @author xlongwei
 *
 */
public class TrainHandler extends AbstractHandler {

	public void info(HttpServerExchange exchange) throws Exception {
		String line = HandlerUtil.getParam(exchange, "line");
		if(StringUtil.isBlank(line)) {
			return;
		}
		JSONObject json = TrainUtil.info(line.toUpperCase());
		if(json != null) {
			HandlerUtil.setResp(exchange, json);
		}
	}
	
	public void query(HttpServerExchange exchange) throws Exception {
		String name = HandlerUtil.getParam(exchange, "station");
		if(!StringUtil.isChinese(name)) {
			return;
		}
		List<String> lines = TrainUtil.stations.get(name);
		JSONObject json = lines(lines);
		if(json != null) {
			HandlerUtil.setResp(exchange, json);
		}
	}
	
	public void search(HttpServerExchange exchange) throws Exception {
		String from = HandlerUtil.getParam(exchange, "from"), to = HandlerUtil.getParam(exchange, "to");
		if(!StringUtil.isChinese(from) || !StringUtil.isChinese(to) || from.startsWith(to) || to.startsWith(from)) {
			return;
		}
		List<String> list1 = TrainUtil.stations.get(from), list2 = TrainUtil.stations.get(to);
		if(CollectionUtils.isNotEmpty(list1) && CollectionUtils.isNotEmpty(list2)) {
			list1.retainAll(list2);
			if(!list1.isEmpty()) {
				list1 = TrainUtil.filter(from, to, list1);
			}
			if(!list1.isEmpty()) {
				//车次 成都东-重庆北
				JSONObject lines = lines(list1);
				if(lines != null) {
					HandlerUtil.setResp(exchange, lines);
					return;
				}
			}
		}
		
		list1 = new LinkedList<>(); list2 = new LinkedList<>();
		for(Entry<String, List<String>> entry : TrainUtil.stations.entrySet()) {
			String key = entry.getKey();
			if(key.startsWith(from)) {
				list1.addAll(entry.getValue());
			} else if(key.startsWith(to)) {
				list2.addAll(entry.getValue());
			}
		}
		if(CollectionUtils.isNotEmpty(list1) && CollectionUtils.isNotEmpty(list2)) {
			list1.retainAll(list2);
			if(!list1.isEmpty()) {
				list1 = TrainUtil.filter(from, to, list1);
			}
			if(!list1.isEmpty()) {
				//车次 成都-重庆
				JSONObject lines = lines(list1);
				if(lines != null) {
					HandlerUtil.setResp(exchange, lines);
					return;
				}
			}
		}
	}
	
	private JSONObject lines(List<String> lines) {
		if(CollectionUtils.isEmpty(lines)) {
			return null;
		}
		JSONObject json = new JSONObject();
		JSONArray array = new JSONArray();
		for(String line : lines) {
			JSONObject item = TrainUtil.info(line);
			if(item!=null) {
				array.add(item);
			}
		}
		json.put("total", lines.size());
		json.put("infos", array);
		return json;
	}
}
