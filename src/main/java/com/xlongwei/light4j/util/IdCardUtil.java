package com.xlongwei.light4j.util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSONArray;
import com.xlongwei.light4j.apijson.DemoApplication;
import com.xlongwei.light4j.util.FileUtil.CharsetNames;
import com.xlongwei.light4j.util.FileUtil.TextReader;
import com.xlongwei.light4j.util.JsonUtil.JsonBuilder;

import lombok.extern.slf4j.Slf4j;

/**
 * idcard util
 * @author xlongwei
 *
 */
@Slf4j
public class IdCardUtil {
	public static final Integer year = 2020;
	public static final Map<String, String> areas = new HashMap<>();

	static {
		if(!DemoApplication.apijsonEnabled){
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
	}

	/** 解析六位行政区划码 */
	public static List<String> areas(String area) {
		List<String> list = new ArrayList<>(3);
		if (StringUtil.isNumbers(area) && area.length()>1) {
			String area1 = area.substring(0, 2) + "0000";
			String area2 = area.length()<4 ? null : area.substring(0, 4) + "00";
			String area3 = area.length()<6 ? null : area.substring(0, 6);
			if(DemoApplication.apijsonEnabled){
				JsonBuilder json = JsonUtil.builder(false);
				json = json.putJSON("Idcard[]").putJSON("Idcard").put("@column", "name").put("@order", "code").putArray("code{}").add(area1);
				if(area2 != null) json.add(area2);
				if(area3 != null) json.add(area3);
				String string  = json.top().build().toJSONString();
				string = DemoApplication.apijson.get(string, null);
				JSONArray array = JsonUtil.parseNew(string).getJSONArray("Idcard[]");
				for(int i=0,s=array==null ? 0 : array.size();i<s;i++){
					list.add(array.getJSONObject(i).getString("name"));
				}
			}else{
				area1 = areas.get(area1);
				area2 = area2==null ? null : areas.get(area2);
				area3 = area3==null ? null : areas.get(area3);
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
		}
		return list;
	}

	public static boolean valid(String area) {
		if(area==null || area.length()!=6) return false;
		if(DemoApplication.apijsonEnabled){
			String string = DemoApplication.apijson.get(JsonUtil.builder(false).putJSON("Idcard").put("@column", "name").put("code", area).put("year", year).top().build().toJSONString(), null);
			return JsonUtil.parseNew(string).getJSONObject("Idcard")!=null;
		}else{
			return areas.containsKey(area);
		}
	}
}
