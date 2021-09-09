//package com.xlongwei.light4j.handler.weixin;
//
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map.Entry;
//import java.util.regex.Pattern;
//
//import org.apache.commons.collections4.CollectionUtils;
//
//import com.alibaba.fastjson.JSONArray;
//import com.alibaba.fastjson.JSONObject;
//import com.xlongwei.light4j.util.AdtUtil.PairList;
//import com.xlongwei.light4j.util.StringUtil;
//import com.xlongwei.light4j.util.TrainUtil;
//import com.xlongwei.light4j.util.TrainUtil.Info;
//import com.xlongwei.light4j.util.TrainUtil.Line;
//import com.xlongwei.light4j.util.WeixinUtil.AbstractMessageHandler.AbstractTextHandler;
//
///**
// * train handler
// * 
// * @author xlongwei
// *
// */
//public class TrainHandler extends AbstractTextHandler {
//
//	private static final String TAG = "车次";
//	private Pattern pattern = Pattern.compile("[a-zA-Z]?\\d{1,4}");
//
//	@Override
//	public String handle(String content) {
//		if (StringUtil.isBlank(content)) {
//			return null;
//		}
//
//		if (pattern.matcher(content).matches()) {
//			return info(content);
//		}
//		if (!content.startsWith(TAG)) {
//			return null;
//		}
//
//		String name = content.substring(2).trim();
//		if (pattern.matcher(name).matches()) {
//			return info(name);
//		}
//
//		List<String> list = TrainUtil.stations.get(name);
//		if (CollectionUtils.isNotEmpty(list)) {
//			return lines(name, list);
//		}
//
//		String[] cmds = name.split("[ -]");
//		int two = 2;
//		if (cmds.length != two) {
//			return null;
//		}
//
//		String from = cmds[0], to = cmds[1];
//		if (!StringUtil.isChinese(from) || !StringUtil.isChinese(to) || from.startsWith(to) || to.startsWith(from)) {
//			return null;
//		}
//		List<String> list1 = TrainUtil.stations.get(from), list2 = TrainUtil.stations.get(to);
//		if (CollectionUtils.isNotEmpty(list1) && CollectionUtils.isNotEmpty(list2)) {
//			list1.retainAll(list2);
//			if (!list1.isEmpty()) {
//				list1 = TrainUtil.filter(from, to, list1);
//			}
//			if (!list1.isEmpty()) {
//				return lines(from, list1); 
//			}
//		}
//
//		list1 = new LinkedList<>();
//		list2 = new LinkedList<>();
//		for (Entry<String, List<String>> entry : TrainUtil.stations.entrySet()) {
//			String key = entry.getKey();
//			if (key.startsWith(from)) {
//				list1.addAll(entry.getValue());
//			} else if (key.startsWith(to)) {
//				list2.addAll(entry.getValue());
//			}
//		}
//		if (CollectionUtils.isNotEmpty(list1) && CollectionUtils.isNotEmpty(list2)) {
//			list1.retainAll(list2);
//			if (!list1.isEmpty()) {
//				list1 = TrainUtil.filter(from, to, list1);
//			}
//			if (!list1.isEmpty()) {
//				return lines(from, list1); 
//			}
//		}
//
//		return null;
//	}
//
//	private String info(String line) {
//		JSONObject train = TrainUtil.info(line.toUpperCase());
//		if (train == null) {
//			return null;
//		}
//		StringBuilder sb = new StringBuilder();
//		String[] keys = { TAG, "列车类型", "始发站", "始发时间", "终点站", "到达时间" };
//		for (String key : keys) {
//			sb.append(key).append("：").append(train.getString(key)).append("\n");
//		}
//		JSONArray stations = train.getJSONArray("经过站点");
//		if (stations != null && !stations.isEmpty()) {
//			keys = new String[] { "车站", "到达时间", "发车时间", "里程（公里）" };
//			sb.append("\n站点，到达，发车，里程\n");
//			for (int i = 0; i < stations.size(); i++) {
//				JSONObject station = stations.getJSONObject(i);
//				for (String key : keys) {
//					sb.append(station.getString(key)).append("，");
//				}
//				sb.append("\n");
//			}
//		}
//		return sb.toString();
//	}
//
//	private String lines(String name, List<String> lines) {
//		if (CollectionUtils.isEmpty(lines)) {
//			return null;
//		}
//		StringBuilder sb = new StringBuilder("车次，发车时间\n");
//		PairList<String, String> pairs = new PairList.PriorityPairList<>();
//		int size = lines.size();
//		for (int i = 0; i < size; i++) {
//			String line = lines.get(i);
//			JSONObject info = TrainUtil.info(line);
//			JSONArray stations = info.getJSONArray(Line.经过站点);
//			for (int j = 0; j < stations.size(); j++) {
//				JSONObject station = stations.getJSONObject(j);
//				if (station.getString(Info.车站).startsWith(name)) {
//					pairs.put(info.getString(Line.车次), station.getString(Info.发车时间));
//					break;
//				}
//			}
//		}
//		while (pairs.moveNext()) {
//			sb.append(pairs.getData()).append("，").append(pairs.getPriority()).append("\n");
//		}
//		if (textTooLong(sb.toString())) {
//			sb.delete(0, sb.length());
//			for (int i = 0; i < size; i++) {
//				if (i > 0 && i % 3 == 0) {
//					sb.append('\n');
//				}
//				sb.append(lines.get(i)).append(' ');
//			}
//			if (textTooLong(sb.toString())) {
//				sb.delete(0, sb.length());
//				for (int i = 0; i < size; i++) {
//					sb.append(lines.get(i));
//				}
//				return textLimited(sb.toString());
//			}
//		}
//		return sb.toString();
//	}
//}
