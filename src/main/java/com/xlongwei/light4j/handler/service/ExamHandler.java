package com.xlongwei.light4j.handler.service;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.JsonUtil;
import com.xlongwei.light4j.util.JsonUtil.JsonBuilder;
import com.xlongwei.light4j.util.NumberUtil;
import com.xlongwei.light4j.util.RedisConfig;
import com.xlongwei.light4j.util.StringUtil;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData.FormValue;
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
					String key = PREFIX+array.getString(i);
					String value = RedisConfig.get(key);
					addQuestion(jsonBuilder, key, value);
				}
			}
		}else if(!StringUtil.isBlank(type)) {
			Map<String, String> map = RedisConfig.gets(RedisConfig.CACHE, "exam."+type+"*");
			map.entrySet().stream().sorted((e1, e2) -> {
				String s1 = e1.getKey(), s2 = e2.getKey();
				Integer v1 = Integer.valueOf(s1.substring(s1.lastIndexOf('.')+1));
				Integer v2 = Integer.valueOf(s2.substring(s1.lastIndexOf('.')+1));
				return v1.compareTo(v2);
			}).forEach(e -> {
				addQuestion(jsonBuilder, e.getKey(), e.getValue());
			});
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
	
	/** 从excel导入问卷
	 * <br/>sheet名称作为type，列标题有：id	question	a	b	c	d	right	note
	 */
	public void excel(HttpServerExchange exchange) throws Exception {
		FormValue file = HandlerUtil.getFile(exchange, "file");
		InputStream is = null;
		if(file!=null && file.isFileItem()) {
			is = file.getFileItem().getInputStream();
		}
		if(is != null) {
			final List<List<String>> rows = new ArrayList<>();
	        ExcelReader excelReader = EasyExcel.read(new BufferedInputStream(is)).autoCloseStream(true).registerReadListener(new AnalysisEventListener<Map<Integer, String>>() {
	            @Override
	            public void invoke(Map<Integer, String> object, AnalysisContext context) {
	                rows.add(object.entrySet().stream().map(Entry::getValue).collect(Collectors.toList()));
	            }
	            @Override
	            public void doAfterAllAnalysed(AnalysisContext context) {
	            }
	        }).build();
	        List<ReadSheet> sheets = excelReader.excelExecutor().sheetList();
	        JsonBuilder jb = JsonUtil.builder(false).put("sheets", sheets.size());
	        Set<String> sheetNames = sheets.stream().map(ReadSheet::getSheetName).collect(Collectors.toSet());
			for(ReadSheet sheet : sheets) {
				jb = handleSheet(sheet, excelReader, sheetNames, rows, jb);
			}
			excelReader.finish();
			HandlerUtil.setResp(exchange, jb.json());
		}
	}

	private JsonBuilder handleSheet(ReadSheet sheet, ExcelReader excelReader, Set<String> sheetNames, List<List<String>> rows, JsonBuilder jb) {
		rows.clear();
		excelReader.read(sheet);
		String sheetName = sheet.getSheetName();
		log.info("read sheet: {} rows: {}",sheetName,rows.size());
		jb = jb.putJSON(sheetName);
		JsonBuilder badRows = JsonUtil.builder(true), paperIds = JsonUtil.builder(true);
		int success = 0;
		for(int i=1;i<rows.size();i++) {
			List<String> row = rows.get(i);
			int idx = 0, cols = row.size();
			String id = cols<=idx?null:row.get(idx++), q = cols<=idx?null:row.get(idx++);
			if(!StringUtil.isBlank(id) && !StringUtil.isBlank(q) && sheetNames.contains(id) && !sheetName.equals(id)) {
				String type = id;
				Integer no = NumberUtil.parseInt(q, null);
				if(no!=null) {
					paperIds.add(type+"."+no);
					success++;
				}else {
					log.info("invalid exam row: {}, sheet: {}", row, sheetName);
					badRows.add(i);
				}
				continue;
			}
			String a = cols<=idx?null:row.get(idx++), b = cols<=idx?null:row.get(idx++), c = cols<=idx?null:row.get(idx++), d = cols<=idx?null:row.get(idx++), r = cols<=idx?null:row.get(idx++), n = cols<=idx?null:row.get(idx++);
			boolean invalid = NumberUtil.parseInt(id, null)==null || StringUtil.isBlank(q) || StringUtil.isBlank(a) || StringUtil.isBlank(b) || StringUtil.isBlank(c)
					|| StringUtil.isBlank(d) || StringUtil.isBlank(r);
			if(invalid == false) {
				String key = PREFIX+sheetName+"."+id;
				String value = JsonUtil.builder(false).put("id", id).put("q", q).put("a", a).put("b", b).put("c", c).put("d", d).put("r", r).put("n", n).json().toJSONString();
				RedisConfig.persist(key, value);
				success++;
			}else {
				log.info("invalid exam row: {}, sheet: {}", row, sheetName);
				badRows.add(i);
			}
		}
		JSONArray paperIdsArray = paperIds.array();
		if(!paperIdsArray.isEmpty()) {
			String key = PREFIX+"paper."+sheetName;
			String value = paperIdsArray.toJSONString();
			RedisConfig.persist(key, value);
		}
		JSONArray badRowsArray = badRows.array();
		if(!badRowsArray.isEmpty()) {
			jb.put("badRows", badRowsArray);
		}
		jb.put("success", success);
		jb = jb.parent();
		return jb;
	}

	private void addQuestion(JsonBuilder jsonBuilder, String key, String value) {
		JSONObject json = JsonUtil.parseNew(value);
		if(!StringUtil.isBlank(json.getString("q"))) {
			String id = key.substring(PREFIX_LENGTH);
			jsonBuilder.addJSON().put("id", id).put("q", json.getString("q")).put("a", json.getString("a")).put("b", json.getString("b")).put("c", json.getString("c")).put("d", json.getString("d")).parent();
		}
	}
	
	
}
