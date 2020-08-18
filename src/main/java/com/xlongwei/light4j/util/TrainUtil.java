package com.xlongwei.light4j.util;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.xlongwei.light4j.util.FileUtil.CharsetNames;
import com.xlongwei.light4j.util.FileUtil.TextReader;

import lombok.extern.slf4j.Slf4j;

/**
 * train util
 * 
 * @author xlongwei
 *
 */
@Slf4j
public class TrainUtil {
	/** K487 =>  {info} */
	public static final Map<String, JSONObject> trains = new HashMap<>(15000);
	/** 重庆  =>  {T10,K587,...} */
	public static final Map<String, List<String>> stations = new HashMap<>(4000);

	public static class Line {
		public static final String 车次 = "车次";
		public static final String 网址 = "网址";
		public static final String 列车类型 = "列车类型";
		public static final String 始发站 = "始发站";
		public static final String 始发时间 = "始发时间";
		public static final String 经过站 = "经过站";
		public static final String 经过站到达时间 = "经过站到达时间";
		public static final String 经过站发车时间 = "经过站发车时间";
		public static final String 终点站 = "终点站";
		public static final String 到达时间 = "到达时间";
		public static final String 经过站点 = "经过站点";
	}

	public static class Info {
		public static final String 车站 = "车站";
		public static final String 到达时间 = "到达时间";
		public static final String 发车时间 = "发车时间";
		public static final String 走行时间 = "走行时间（小时）";
		public static final String 里程 = "里程（公里）";
	}

	public static List<String> filter(String from, String to, List<String> lines) {
		List<String> list2 = new LinkedList<>();
		for (String line : lines) {
			JSONObject info = TrainUtil.trains.get(line);
			JSONArray stations = info.getJSONArray(Line.经过站点);
			boolean fromFind = false, toFind = false;
			for (int i = 0; i < stations.size(); i++) {
				JSONObject station = stations.getJSONObject(i);
				String stationName = station.getString(Info.车站);
				if (stationName.startsWith(from)) {
					if (toFind) {
						break;
					} else {
						fromFind = true;
					}
				} else if (stationName.startsWith(to)) {
					if (fromFind) {
						toFind = true;
					} else {
						break;
					}
				}
			}
			if (fromFind && toFind) {
				list2.add(line);
			}
		}
		return list2;
	}

	static {
		String line = null;
		File train = new File(ConfigUtil.DIRECTORY, "trains.json");
		log.info("train file: {}", train);
		TextReader reader = new TextReader(train, CharsetNames.UTF_8);
		while ((line = reader.read()) != null) {
			JSONObject info = JSON.parseObject(line);
			String key = info.getString(Line.车次);
			if (StringUtil.hasLength(key)) {
				trains.put(key, info);
				initStations(info);
			}
		}
		reader.close();
		log.info("train init success");
	}

	private static void initStations(JSONObject train) {
		String line = train.getString(Line.车次);
		if (StringUtil.isBlank(line)) {
			return;
		}
		JSONArray infos = train.getJSONArray(Line.经过站点);
		if (infos != null && !infos.isEmpty()) {
			for (int i = 0; i < infos.size(); i++) {
				JSONObject info = infos.getJSONObject(i);
				String name = info.getString(Info.车站);
				List<String> lines = stations.get(name);
				if (lines == null) {
					stations.put(name, lines = new LinkedList<>());
				}
				lines.add(line);
			}
		}
	}

}
