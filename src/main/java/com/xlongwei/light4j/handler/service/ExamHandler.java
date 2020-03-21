package com.xlongwei.light4j.handler.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.JsonUtil;
import com.xlongwei.light4j.util.JsonUtil.JsonBuilder;
import com.xlongwei.light4j.util.RedisConfig;
import com.xlongwei.light4j.util.StringUtil;

import io.undertow.server.HttpServerExchange;
import lombok.extern.slf4j.Slf4j;

/**
 * exam handler
 * @author xlongwei
 *
 */
@Slf4j
public class ExamHandler extends AbstractHandler {
	private static final String PREFIX = "exam.";
	private static final int PREFIX_LENGTH = PREFIX.length();
	
	/** 获取试卷
	 * @param type 试卷类型 exam.{java1}.*={q,a,b,c,d,r,n}
	 * @param id 试卷编号 exam.paper.{id}={type:[id]}
	 * @param answer 回答编号，可以查看问卷回答清空 exam.answer.{id}={type.id:answer}
	 */
	public void paper(HttpServerExchange exchange) throws Exception {
		String id = HandlerUtil.getParam(exchange, "id"), type = HandlerUtil.getParam(exchange, "type"), answer = HandlerUtil.getParam(exchange, "answer");
		JsonBuilder jsonBuilder = JsonUtil.builder(false).putArray("questions"), answerBuilder = JsonUtil.builder(false);;
		if(!StringUtil.isBlank(id)) {
			String paper = RedisConfig.get("exam.paper."+id);
			JSONArray array = JsonUtil.parseArray(paper);
			if(array!=null && !array.isEmpty()) {
				for(int i=0;i<array.size();i++) {
					String key = array.getString(i);
					String value = RedisConfig.get(PREFIX+key);
					addQuestion(jsonBuilder, key, value);
				}
			}
		}else if(!StringUtil.isBlank(type)) {
			Map<String, String> map = RedisConfig.gets(RedisConfig.CACHE, "exam."+type+"*");
			List<String> keys = new ArrayList<>(map.keySet());
			Collections.sort(keys, ((o1,o2) -> {
				String s1 = (String)o1, s2 = (String) o2;
				Integer v1 = Integer.valueOf(s1.substring(s1.lastIndexOf('.')+1));
				Integer v2 = Integer.valueOf(s2.substring(s1.lastIndexOf('.')+1));
				return v1.compareTo(v2);
			}));
			for(String key : keys) {
				String value = map.get(key);
				addQuestion(jsonBuilder, key, value);
			}
		}else if(!StringUtil.isBlank(answer)) {
			String answers = RedisConfig.get(PREFIX+"answer."+answer);
			JSONObject json = JsonUtil.parseNew(answers);
			if(!json.isEmpty()) {
				answerBuilder.put("answerId", answer);
				int corrects = 0;
				for(String typeId : json.keySet()) {
					String key = PREFIX+typeId;
					String value = RedisConfig.get(key);
					JSONObject question = JsonUtil.parseNew(value);
					if(!question.isEmpty()) {
						addQuestion(jsonBuilder, key, value);
						//添加回答信息
						answer = json.getString(typeId);
						if(!question.getString("r").equals(answer)) {
							answerBuilder.putJSON(typeId).put("a", answer).put("r", question.getString("r")).put("n", question.getString("n")).parent();
						}else {
							corrects += 1;
							answerBuilder.putJSON(typeId).put("a", answer).parent();
						}
					}
				}
				answerBuilder.put("corrects", corrects);
			}
		}
		JSONObject json = jsonBuilder.parent().json();
		JSONObject ret = answerBuilder.json();
		if(!ret.isEmpty()) {
			json.put("result", ret);
		}
		HandlerUtil.setResp(exchange, json);
	}
	
	/** 提交问卷
	 * @param answers {type.id:answer}
	 * @return {type.id:{r,n}} 返回错题的答案和提示
	 */
	public void commit(HttpServerExchange exchange) throws Exception {
		String bodyString = HandlerUtil.getBodyString(exchange);
		String answers = StringUtil.firstNotBlank(JsonUtil.parseNew(bodyString).getString("answers"), HandlerUtil.getParam(exchange, "answers"));
		JSONObject json = JsonUtil.parseNew(answers);
		JsonBuilder jsonBuilder = JsonUtil.builder(false);
		int corrects = 0;
		for(String typeId : json.keySet()) {
			String answer = json.getString(typeId);
			JSONObject question = JsonUtil.parseNew(RedisConfig.get(PREFIX+typeId));
			if(!question.isEmpty()) {
				if(!question.getString("r").equals(answer)) {
					jsonBuilder.putJSON(typeId).put("r", question.getString("r")).put("n", question.getString("n")).parent();
				}else {
					corrects += 1;
				}
			}
		}
		JSONObject ret = jsonBuilder.json();
		if(!json.isEmpty() && (corrects>0 || !ret.isEmpty())) {
			String dateKey = LocalDate.now().toString().replaceAll("-", "");
			if(RedisConfig.get(PREFIX+"answer."+dateKey)!=null) {
				int i = 1;
				while(RedisConfig.get(PREFIX+"answer."+dateKey+"."+i)!=null) {
					i++;
				}
				dateKey += "."+i;
			}
			RedisConfig.persist(PREFIX+"answer."+dateKey, answers);
			log.info("exam answer committed: {}, corrects: {}, wrongs: {}", dateKey, corrects, ret.size());
			ret.put("answerId", dateKey);
		}
		ret.put("corrects", corrects);
		HandlerUtil.setResp(exchange, JsonUtil.builder(false).put("result", ret).json());
	}

	private void addQuestion(JsonBuilder jsonBuilder, String key, String value) {
		String id;
		JSONObject json = JsonUtil.parseNew(value);
		if(!StringUtil.isBlank(json.getString("q"))) {
			id = key.substring(PREFIX_LENGTH);
			jsonBuilder.addJSON()
					.put("id", id)
					.put("q", json.getString("q"))
					.put("a", json.getString("a"))
					.put("b", json.getString("b"))
					.put("c", json.getString("c"))
					.put("d", json.getString("d"))
				.parent();
		}
	}
	
	
}
