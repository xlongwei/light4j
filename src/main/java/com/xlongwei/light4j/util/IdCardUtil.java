package com.xlongwei.light4j.util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.xlongwei.light4j.util.FileUtil.CharsetNames;
import com.xlongwei.light4j.util.FileUtil.TextReader;

import lombok.extern.slf4j.Slf4j;

/**
 * idcard util
 * @author xlongwei
 *
 */
@Slf4j
public class IdCardUtil {
	public static final Map<String, String> areas = new HashMap<>();

	static {
		try(InputStream inputStream = ConfigUtil.stream("idcard.txt")) {
			TextReader reader = new TextReader();
			reader.open(inputStream, CharsetNames.UTF_8);
			String line = null;
			while ((line = reader.read()) != null) {
				if (StringUtil.isBlank(line) || line.startsWith("#")) {
					continue;
				}
				String[] split = StringUtils.split(line);
				if (split == null || split.length != 2) {
					continue;
				}
				areas.put(split[0], split[1]);
			}
			reader.close();
			log.info("idcard areas initialized, total areas: {}", areas.size());
		}catch(Exception e) {
			log.info("fail to init idcard.txt, ex: {}", e.getMessage());
		}
	}

	/** 解析六位行政区划码 */
	public static List<String> areas(String area) {
		List<String> list = new ArrayList<>(3);
		if (StringUtil.isNumbers(area) && area.length()>1) {
			String area1 = areas.get(area.substring(0, 2) + "0000");
			String area2 = area.length()<4 ? null : areas.get(area.substring(0, 4) + "00");
			String area3 = area.length()<6 ? null : areas.get(area.substring(0, 6));
			if (StringUtil.hasLength(area1)) {
				list.add(area1);
			}
			if (StringUtil.hasLength(area2)) {
				list.add(area2);
			}
			if (StringUtil.hasLength(area3)) {
				list.add(area3);
			}
		}
		return list;
	}

}
