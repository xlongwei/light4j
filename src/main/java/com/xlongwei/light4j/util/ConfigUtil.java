package com.xlongwei.light4j.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.utility.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * light4j.yml
 * @author xlongwei
 *
 */
@Slf4j
@SuppressWarnings("unchecked")
public class ConfigUtil {

	public static final Map<String, Object> CONFIG = Config.getInstance().getJsonMapConfig("light4j");
	/** light4j自定义配置 */
	public static final Map<String, String> LIGHT4J, REDIS, UPLOAD, WEIXIN;
	
	static {
		//使用env或properties覆盖自定义配置
		Map<String, String> replace = new HashMap<>(4);
		for(String key : CONFIG.keySet()) {
			Object value = CONFIG.get(key);
			if(value instanceof Map) {
				Map<String, String> map = (Map<String, String>)value;
				replace.clear();
				for(String name : map.keySet()) {
					String keyName = key+"."+name;
					String keyValue = System.getenv(keyName);
					if(StringUtil.isBlank(keyValue)) {
						keyValue = System.getProperty(keyName);
					}
					if(StringUtil.isBlank(keyValue)==false) {
						replace.put(keyName, keyValue);
						log.info("config {} => {}", keyName, keyValue);
					}
				}
				map.putAll(replace);
			}
		}
		log.info("config load success");
		LIGHT4J = (Map<String, String>)CONFIG.get("light4j");
		REDIS = (Map<String, String>)CONFIG.get("redis");
		UPLOAD = (Map<String, String>)CONFIG.get("upload");
		WEIXIN = (Map<String, String>)CONFIG.get("weixin");
		DIRECTORY = LIGHT4J.get("directory");
	}
	
	public static final String DIRECTORY;
	
	public static final TypeReference<Map<String, Integer>> STRING_MAP_INTEGER = new TypeReference<Map<String, Integer>>() {};
	public static final TypeReference<Map<String, Object>> STRING_MAP_OBJECT = new TypeReference<Map<String, Object>>() {};
	
	public static InputStream stream(String resource) {
		if(StringUtils.isBlank(resource)) {
			return null;
		}else {
			String url = DIRECTORY + resource;
			try {
				if(StringUtil.isUrl(url)) {
					return new URL(url).openStream();
				}else {
					File file = new File(url);
					if(file.exists() && file.isFile()) {
						return new FileInputStream(file);
					}else {
						log.info("file not exist or is not file, resource: {}, exists: {}, isFile: {}", url, file.exists(), file.isFile());
					}
				}
			}catch(Exception e) {
				log.info("fail to get resource: {}, ex: {}", resource, e.getMessage());
			}
		}
		return null;
	}
	
	public static Map<String, Integer> stringMapInteger(String json) {
		try {
			return Config.getInstance().getMapper().readValue(json, STRING_MAP_INTEGER);
		}catch(Exception e) {
			return null;
		}
	}
	
	public static Map<String, Object> stringMapObject(String json) {
		try {
			return Config.getInstance().getMapper().readValue(json, STRING_MAP_OBJECT);
		}catch(Exception e) {
			return null;
		}
	}
}
