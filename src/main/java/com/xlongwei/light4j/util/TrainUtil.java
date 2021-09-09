//package com.xlongwei.light4j.util;
//
//import java.io.BufferedReader;
//import java.io.ByteArrayInputStream;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
//import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
//
//import com.alibaba.fastjson.JSON;
//import com.alibaba.fastjson.JSONArray;
//import com.alibaba.fastjson.JSONObject;
//import com.xlongwei.light4j.util.FileUtil.CharsetNames;
//import com.xlongwei.light4j.util.FileUtil.TextReader;
//
//import cn.hutool.cache.impl.LFUCache;
//import lombok.extern.slf4j.Slf4j;
//
///**
// * train util
// * 
// * @author xlongwei
// *
// */
//@Slf4j
//public class TrainUtil {
//	/** K487 =>  {info} */
//	private static final Map<String, String> trains = new HashMap<>(16384);
//	/** 重庆  =>  {T10,K587,...} */
//	public static final Map<String, List<String>> stations = new HashMap<>(4096);
//	/** trains.tgz更省内存 */
//	private static final InputStream tgzStream = ConfigUtil.stream("trains.tgz");
//	private static final boolean tgzEnabled = NumberUtil.parseBoolean(System.getProperty("trainsTgz"), tgzStream!=null);
//	private static byte[] tgzBytes = null;
//	private static final Set<String> lines = tgzEnabled ? new HashSet<>(16384) : null;
//	private static final LFUCache<String, String> cache = tgzEnabled ? new LFUCache<>(1024) : null;
//
//	public static class Line {
//		public static final String 车次 = "车次";
//		public static final String 网址 = "网址";
//		public static final String 列车类型 = "列车类型";
//		public static final String 始发站 = "始发站";
//		public static final String 始发时间 = "始发时间";
//		public static final String 经过站 = "经过站";
//		public static final String 经过站到达时间 = "经过站到达时间";
//		public static final String 经过站发车时间 = "经过站发车时间";
//		public static final String 终点站 = "终点站";
//		public static final String 到达时间 = "到达时间";
//		public static final String 经过站点 = "经过站点";
//	}
//
//	public static class Info {
//		public static final String 车站 = "车站";
//		public static final String 到达时间 = "到达时间";
//		public static final String 发车时间 = "发车时间";
//		public static final String 走行时间 = "走行时间（小时）";
//		public static final String 里程 = "里程（公里）";
//	}
//	
//	public static JSONObject info(String line) {
//		if(tgzEnabled) {
//			if(lines.contains(line)) {
//				JSONObject parse = JsonUtil.parse(cache.get(line));
//				if(parse != null) {
//					return parse;
//				}
//				try(BufferedReader reader = new BufferedReader(new InputStreamReader(tgzStream(tgzBytes), CharsetNames.UTF_8))) {
//					String row = null;
//					while((row = reader.readLine())!=null) {
//						JSONObject info = JSON.parseObject(row);
//						String key = info.getString(Line.车次);
//						if(line.equals(key)) {
//							cache.put(key, row);
//							return info;
//						}
//					}
//				}catch (Exception e) {
//					log.warn("{} {}", e.getClass().getSimpleName(), e.getMessage());
//				}
//			}
//		}else {
//			return JsonUtil.parse(trains.get(line));
//		}
//		return null;
//	}
//	
//	public static List<String> station(String station) {
//		return stations.get(station);
//	}
//
//	public static List<String> filter(String from, String to, List<String> lines) {
//		List<String> list2 = new LinkedList<>();
//		for (String line : lines) {
//			JSONObject info = info(line);
//			JSONArray stations = info.getJSONArray(Line.经过站点);
//			boolean fromFind = false, toFind = false;
//			for (int i = 0; i < stations.size(); i++) {
//				JSONObject station = stations.getJSONObject(i);
//				String stationName = station.getString(Info.车站);
//				if (stationName.startsWith(from)) {
//					if (toFind) {
//						break;
//					} else {
//						fromFind = true;
//					}
//				} else if (stationName.startsWith(to)) {
//					if (fromFind) {
//						toFind = true;
//					} else {
//						break;
//					}
//				}
//			}
//			if (fromFind && toFind) {
//				list2.add(line);
//			}
//		}
//		return list2;
//	}
//
//	static {
//		String line = null;
//		if(tgzEnabled) {
//			tgzBytes = FileUtil.readStream(tgzStream).toByteArray();
//			log.info("trainsTgz={}", tgzBytes.length);
//		}
//		TextReader reader = new TextReader(tgzEnabled ? tgzStream(tgzBytes) : ConfigUtil.stream("trains.json"), CharsetNames.UTF_8);
//		while ((line = reader.read()) != null) {
//			JSONObject info = JSON.parseObject(line);
//			String key = info.getString(Line.车次);
//			if (StringUtil.hasLength(key)) {
//				if(tgzEnabled) {
//					lines.add(key);
//				}else {
//					trains.put(key, line);
//				}
//				initStations(info);
//			}
//		}
//		reader.close();
//		log.info("train init success");
//	}
//
//	private static void initStations(JSONObject train) {
//		String line = train.getString(Line.车次);
//		if (StringUtil.isBlank(line)) {
//			return;
//		}
//		JSONArray infos = train.getJSONArray(Line.经过站点);
//		if (infos != null && !infos.isEmpty()) {
//			for (int i = 0; i < infos.size(); i++) {
//				JSONObject info = infos.getJSONObject(i);
//				String name = info.getString(Info.车站);
//				List<String> lines = stations.get(name);
//				if (lines == null) {
//					stations.put(name, lines = new LinkedList<>());
//				}
//				lines.add(line);
//			}
//		}
//	}
//
//	private static InputStream tgzStream(byte[] tgzBytes) {
//		try{
//			TarArchiveInputStream tgz = new TarArchiveInputStream(new GzipCompressorInputStream(new ByteArrayInputStream(tgzBytes)));
//			tgz.getNextEntry();//trains.json
//			return tgz;
//		}catch(Exception e) {
//			log.warn("{} {}", e.getClass().getSimpleName(), e.getMessage());
//			return null;
//		}
//	}
//
//}
