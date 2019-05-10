package com.xlongwei.light4j.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
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
	
	public static final String DIRECTORY = System.getProperty("light4j.directory", ConfigUtil.light4j().get("directory"));
	
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
	
	public static Map<String, String> light4j() {
		return (Map<String, String>)CONFIG.get("light4j");
	}
	
	public static Map<String, String> redis() {
		return (Map<String, String>)CONFIG.get("redis");
	}
	
	public static Map<String, String> weixin() {
		return (Map<String, String>)CONFIG.get("weixin");
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
