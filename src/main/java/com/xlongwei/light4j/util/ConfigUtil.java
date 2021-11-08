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
						replace.put(name, keyValue);
						log.info("config {} => {}", keyName, keyValue);
					}
				}
				map.putAll(replace);
			}
		}
		log.info("light4j config loaded");
		Map<String, String> light4j = config("light4j");
		DIRECTORY = light4j.get("directory");
		FRONT_URL = light4j.get("frontUrl");
	}
	
	public static final String DIRECTORY, FRONT_URL;
	
	public static final TypeReference<Map<String, Integer>> STRING_MAP_INTEGER = new TypeReference<Map<String, Integer>>() {};
	public static final TypeReference<Map<String, Object>> STRING_MAP_OBJECT = new TypeReference<Map<String, Object>>() {};
	
	public static InputStream stream(String resource) {
		if(StringUtils.isBlank(resource)) {
			return null;
		}else {
			try {
				if(StringUtil.isUrl(DIRECTORY)) {
					return new URL(DIRECTORY+resource).openStream();
				}else {
					File file = new File(DIRECTORY, resource);
					if(file.exists() && file.isFile()) {
						return new FileInputStream(file);
					}else {
						log.info("file not exist or is not file, resource: {}, exists: {}, isFile: {}", resource, file.exists(), file.isFile());
					}
				}
			}catch(Exception e) {
				log.info("fail to get resource: {}, ex: {}", resource, e.getMessage());
			}
		}
		return null;
	}
	
	public static Map<String, String> config(String name) {
		return (Map<String, String>)CONFIG.get(name);
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
	
	/** 判断用户是否客户 */
	public static boolean isClient(String userName) {
		String clientNames = RedisConfig.get("liveqrcode.client.names");
		boolean isClient = !StringUtil.isBlank(userName) && StringUtil.splitContains(clientNames, userName);
		log.info("userName: {}, isClient: {}, clientNames: {}", userName, isClient, clientNames);
		return isClient;
	}
}
