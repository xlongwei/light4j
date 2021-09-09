//package com.xlongwei.light4j.handler.weixin;
//
//import java.io.BufferedInputStream;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//
//import com.xlongwei.light4j.util.ConfigUtil;
//import com.xlongwei.light4j.util.StringUtil;
//import com.xlongwei.light4j.util.WeixinUtil.AbstractMessageHandler.AbstractTextHandler;
//
//import cn.hutool.core.util.NumberUtil;
//import cn.hutool.poi.excel.ExcelUtil;
//import lombok.extern.slf4j.Slf4j;
//
///**
// * poetry handler
// * @author xlongwei
// *
// */
//@Slf4j
//public class PoetryHandler extends AbstractTextHandler {
//	
//	private static final String TAG = "唐诗";
//
//	@Override
//	public String handle(String content) {
//		if(StringUtil.isBlank(content) || !content.startsWith(TAG) || StringUtil.isBlank(content=content.substring(TAG.length()))) {
//				return null;
//		}
//		return search(content);
//	}
//	
//	public static String search(String str) {
//		if(str == null || (str=str.trim()).length()==0) {
//			return null;
//		}
//		if(StringUtil.isNumbers(str)) {
//			return poetry(poetrys.get((NumberUtil.parseInt(str)-1) % poetrys.size()));
//		}
//		List<Integer> authors = new ArrayList<>(), rows = new ArrayList<>();
//		for(int i=0;i<poetrys.size();i++) {
//			String[] row = poetrys.get(i);
//			if(row[1].equals(str)) {
//				return poetry(row);
//			}else if(row[2].equals(str)) {
//				authors.add(i);
//			}else if(row[2].contains(str) || row[3].contains(str)) {
//				rows.add(i);
//			}
//		}
//		if(authors.isEmpty() && rows.isEmpty()) {
//			return null;
//		}else if(authors.size()==1) {
//			return poetry(poetrys.get(authors.get(0)));
//		}else if(rows.size()==1) {
//			return poetry(poetrys.get(rows.get(0)));
//		}else {
//			authors.addAll(rows);
//			Collections.sort(authors);
//			StringBuilder sb = new StringBuilder();
//			for(int i : authors) {
//				String[] row = poetrys.get(i);
//				sb.append("唐诗").append(i+1).append("：").append(row[1]).append("\n");
//			}
//			return sb.toString();
//		}
//	}
//	
//	private static String poetry(String[] row) {
//		return new StringBuilder(row[1]).append(" —— ").append(row[2]).append("\n").append(row[3].replaceAll("([，。])", "$1\n")).toString();
//	}
//
//	/** List[type,title,author,body] */
//	static List<String[]> poetrys = new ArrayList<>();
//	static {
//		try{
//			ExcelUtil.readBySax(new BufferedInputStream(ConfigUtil.stream("ts300.xlsx")), 0, (int i, long j, List<Object> list) -> {
//					if(j > 0) {
//						poetrys.add(list.toArray(new String[list.size()]));
//					}
//			});
//			log.info("poetrys loaded, record={}", poetrys.size());
//		}catch(Exception e) {
//			log.warn("fail to init poetrys: {}", e.getMessage());
//		}
//	}
//}
