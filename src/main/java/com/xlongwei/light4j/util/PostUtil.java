//package com.xlongwei.light4j.util;
//
//import com.alibaba.fastjson.JSON;
//import com.alibaba.fastjson.JSONObject;
//import com.xlongwei.light4j.util.FileUtil.CharsetNames;
//import com.xlongwei.light4j.util.FileUtil.TextReader;
//import com.xlongwei.light4j.util.RelationUtil.Relation1N;
//
//import lombok.extern.slf4j.Slf4j;
//
///** 
// * 邮编区号
// * @author xlongwei
// */
//@Slf4j
//public class PostUtil {
//	public static final Relation1N<String, String> posts = new Relation1N<>();
//	public static final Relation1N<String, String> areas = new Relation1N<>();
//	
//	public static class Info {
//		public static final String 区号 = "区号";
//		public static final String 邮编 = "邮编";
//		public static final String 城市 = "城市";
//	}
//	
//	static {
//		String line = null;
//		TextReader reader = new TextReader(ConfigUtil.stream("posts.json"), CharsetNames.UTF_8);
//		while((line=reader.read())!=null) {
//				JSONObject info = JSON.parseObject(line);
//				String post = info.getString(Info.邮编);
//				String area = info.getString(Info.区号);
//				String city = info.getString(Info.城市);
//				posts.add(post, city);
//				areas.add(area, city);
//		}
//		reader.close();
//		log.info("post init success");
//	}
//}
