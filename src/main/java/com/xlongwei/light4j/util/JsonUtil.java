package com.xlongwei.light4j.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import lombok.extern.slf4j.Slf4j;

/**
 * json util
 * @author xlongwei
 *
 */
@Slf4j
public class JsonUtil {
	
	public static boolean getBoolean(JSONObject json, String path) {
		return NumberUtil.parseBoolean(get(json, path), false);
	}
	
	/** default -1111 */
	public static int getInt(JSONObject json, String path) {
		return NumberUtil.parseInt(get(json, path), -1111);
	}
	
	/** policy_set[0].hit_rules[0].decision */
	public static String get(JSONObject json, String path) {
		if(json==null || StringUtil.isBlank(path)) {
			return null;
		}
		String[] paths = path.split("[\\.]");
		for(int i=0; i<paths.length; i++) {
			if(json == null) {
				return null;
			}
			path = paths[i];
			int pos1 = path.indexOf('['), pos2 = pos1>-1 ? path.indexOf(']', pos1+1) : -1;
			if(i==paths.length-1) {
				if(pos1 == -1) {
					return json.getString(path);
				}else {
					String name = path.substring(0, pos1);
					int idx = Integer.parseInt(path.substring(pos1+1, pos2));
					JSONArray jsonArray = json.getJSONArray(name);
					return jsonArray!=null && jsonArray.size() > idx ? jsonArray.getString(idx) : null;
				}
			}else {
				if(pos1==-1) {
					json = json.getJSONObject(path);
				}else {
					String name = path.substring(0, pos1);
					int idx = Integer.parseInt(path.substring(pos1+1, pos2));
					JSONArray jsonArray = json.getJSONArray(name);
					json = jsonArray!=null && jsonArray.size() > idx ? jsonArray.getJSONObject(idx) : null;
				}
			}
		}
		return null;
	}
	
	public static boolean getBoolean(String json, String path) {
		return NumberUtil.parseBoolean(get(json, path), false);
	}
	
	public static int getInt(String json, String path) {
		return NumberUtil.parseInt(get(json, path), -1111);
	}
	
	public static String get(String json, String path) {
		if(StringUtil.isBlank(json) || StringUtil.isBlank(path)) {
			return null;
		}
		return get(parse(json), path);
	}
	
	public static <T> List<T> getList(String json, String path, Class<T> clazz) {
		if(StringUtil.isBlank(json) || StringUtil.isBlank(path)) {
			return null;
		}
		return JSON.parseArray(JsonUtil.get(json, path), clazz);
	}
	
	public static JSONObject getJson(JSONObject json, String path) {
		return parse(get(json, path));
	}
	
	public static JSONObject getJson(String json, String path) {
		return parse(get(json, path));
	}
	
	public static JSONArray getArray(JSONObject json, String path) {
		return parseArray(get(json, path));
	}
	
	public static JSONArray getArray(String json, String path) {
		return parseArray(get(json, path));
	}
	
	/** parse json to JSONObject, default null */
	public static JSONObject parse(String json) {
		if(StringUtil.hasLength(json)) {
			try {
				return JSON.parseObject(json);
			}catch(Exception e) {
				log.warn("fail to parse exception content: "+json);
			}
		}
		return null;
	}
	
	public static JSONArray parseArray(String json) {
		if(StringUtil.hasLength(json)) {
			try {
				return JSON.parseArray(json);
			}catch(Exception e) {
				log.warn("fail to parse json array: "+json);
			}
		}
		return null;
	}
	
	/** parse json to type T, default null */
	public static <T> T parse(String json, Class<T> clazz){
		if(StringUtil.hasLength(json)) {
			try {
				return JSON.parseObject(json, clazz);
			}catch(Exception e) {
				log.warn("fail to parse exception content: "+json);
			}
		}else {
			log.warn("fail to parse empty or bad content: "+json);
		}
		return null;
	}
	
	/** convert map to json */
	public static JSONObject convert(Map<String, String> params) {
		JSONObject json = new JSONObject();
		if(params!=null && params.size()>0) {
			for(String param:params.keySet()) {
				String value=params.get(param);
				json.put(param, value);
			}
		}
		return json;
	}
	
	/** convert json to map */
	public static Map<String, String> convert(JSONObject json) {
		Map<String, String> map = new HashMap<>(2);
		Set<Entry<String, Object>> entrySet = json.entrySet();
		for(Entry<String, Object> entry : entrySet) {
			map.put(entry.getKey(), String.valueOf(entry.getValue()));
		}
		return map;
	}
	
	/** 解析json串，必要时新建空对象 */
	public static JSONObject parseNew(String json) {
		try{
			JSONObject parse = JSONObject.parseObject(json);
			if(parse != null) {
				return parse;
			}
		}catch(Exception e) {}
		return new JSONObject();
	}
	
	/** modify json string directly */
	public static String append(String json, Map<String, String> params) {
		JSONObject parse = parse(json);
		for(String param : params.keySet()) {
			parse.put(param, params.get(param));
		}
		return parse.toJSONString();
	}
	
	/** @param isArray true=构建JSONArray false=构建JSONObject */
	public static JsonBuilder builder(boolean isArray) { return new JsonBuilder(null, isArray); }
	
	/** build kvPairs to JSONObject */
	public static JSONObject build(String ... kvPairs) { return (JSONObject)new JsonBuilder(null, false).puts(kvPairs).build(); }
	
	/**
	 * <p>put*用于构建JSONObject，add*用于构建JSONArray，putJSON、addArray、parent、top用于层级变动，build、json、array构建最终结果
	 * <p>
	 * <code>
	 * <p>JsonUtil.build(false).put(key, value).putJSON(key, kvPairs).putArray(key, items).json();
	 * <p>JsonUtil.build(true).add(item).addArray(items).addJSON(kvPairs).array();
	 * <p>层级变动时需要记住当前层级，最后可以直接top().build()
	 * <p>JsonUtil.build(false).putArray(key).addArray(items).parent().putJSON(key).put(key, value).top().json();
	 * <p>JsonUtil.build(true).addJSON().put(key, value).parent().addArray().add(item).top().array();
	 * </code>
	 */
	public static class JsonBuilder {
		private static final int TWO = 2;
		private JSONObject jsonObject; private JSONArray jsonArray; private JsonBuilder parent;
		public JsonBuilder(JsonBuilder parent, boolean isArray) { this.parent = parent; if(isArray) { jsonArray = new JSONArray(); } else { jsonObject = new JSONObject(); } }
		public JSON build() { return jsonObject != null ? jsonObject : jsonArray; }
		public JSONObject json() { return jsonObject != null ? jsonObject : null; }
		public JSONArray array() { return jsonObject != null ? null : jsonArray; }
		public JsonBuilder put(String key, Object value) { if(jsonObject != null) { jsonObject.put(key, value); } return this; }
		public JsonBuilder puts(String ... kvPairs) { if(jsonObject != null) { for(int i=1;i<kvPairs.length;i+=TWO) { jsonObject.put(kvPairs[i-1], kvPairs[i]); } } return this; }
		public JsonBuilder putArray(String key) { if(jsonObject != null) { JsonBuilder array = new JsonBuilder(this, true); jsonObject.put(key, array.build()); return array; } else { return null; }}
		public JsonBuilder putArray(String key, String ... items) { if(jsonObject!=null && items.length>0) { JSONArray array = new JSONArray(); for(String item:items) {array.add(item);} jsonObject.put(key, array); } return this; }
		public JsonBuilder putJSON(String key) { JsonBuilder json = new JsonBuilder(this, false); if(jsonObject != null) { jsonObject.put(key, json.build()); } else { jsonArray.add(json.build()); } return json;}
		public JsonBuilder putJSON(String key, String ... kvPairs) { if(jsonObject!=null && kvPairs.length>1) { JSONObject json = new JSONObject(); for(int i=1;i<kvPairs.length;i+=TWO) { json.put(kvPairs[i-1], kvPairs[i]); } jsonObject.put(key, json); } return this; }
		public JsonBuilder add(Object item) { if(jsonArray != null) { jsonArray.add(item); } return this; }
		public JsonBuilder adds(Object ... items) { if(jsonArray != null) { for(Object item:items) { jsonArray.add(item); } } return this; }
		public JsonBuilder addArray() { if(jsonArray != null) { JsonBuilder array = new JsonBuilder(this, true); jsonArray.add(array.build()); return array; } return this; }
		public JsonBuilder addArray(Object ... items) { if(jsonArray!=null && items.length>0) { JSONArray array = new JSONArray(); for(Object item:items) { array.add(item); } jsonArray.add(array); } return this; }
		public JsonBuilder addJSON() { if(jsonArray != null) { JsonBuilder json = new JsonBuilder(this, false); jsonArray.add(json.build()); return json; } return this; }
		public JsonBuilder addJSON(String ... kvPairs) { if(jsonArray!=null && kvPairs.length>1) { JSONObject json = new JSONObject(); for(int i=1;i<kvPairs.length;i+=TWO) { json.put(kvPairs[i-1], kvPairs[i]); } jsonArray.add(json); } return this; }
		public JsonBuilder top() { if(parent == null) { return this; } JsonBuilder top = this; while(top.parent != null) { top = top.parent; } return top; }
		public JsonBuilder parent() { return parent; }
	}
}