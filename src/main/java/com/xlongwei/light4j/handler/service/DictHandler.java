//package com.xlongwei.light4j.handler.service;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
//import com.xlongwei.light4j.util.DictUtil;
//import com.xlongwei.light4j.util.DictUtil.WordScore;
//import com.xlongwei.light4j.util.HandlerUtil;
//import com.xlongwei.light4j.util.PinyinUtil;
//import com.xlongwei.light4j.util.StringUtil;
//
//import io.undertow.server.HttpServerExchange;
//
///**
// * dict handler
// * @author xlongwei
// *
// */
//public class DictHandler extends AbstractHandler {
//
//	@Override
//	public void handleRequest(HttpServerExchange exchange) throws Exception {
//		String parts = HandlerUtil.getParam(exchange, "parts");
//		if(!StringUtil.isBlank(parts)) {
//			List<WordScore> list = DictUtil.parse(parts);
//			if(list!=null && list.size()>0) {
//				Map<String, Object> map = new HashMap<>(1);
//				List<Object> array = new ArrayList<>();
//				for(WordScore word : list) {
//					Map<String, Object> item = new HashMap<>(list.size());
//					item.put("word", word.getWord());
//					item.put("pinyin", PinyinUtil.getPinyin(word.getWord(), 0, 0, 0)[0]);
//					array.add(item);
//					if(array.size() >= 10) {
//						break;
//					}
//				}
//				map.put("words", array);
//				HandlerUtil.setResp(exchange, map);
//			}
//		}
//	}
//
//}
