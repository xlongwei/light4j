package com.xlongwei.light4j.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;

import com.networknt.utility.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Resources {
	public static final String directory = System.getProperty("light4j.directory", "http://t.xlongwei.com/softwares/library/");

	public static InputStream getResource(String resource) {
		if(StringUtils.isBlank(resource)) {
			return null;
		}else {
			String url = directory + resource;
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
}
