package com.xlongwei.light4j;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.xlongwei.light4j.util.FileUtil;
import com.xlongwei.light4j.util.FileUtil.CharsetNames;
import com.xlongwei.light4j.util.HtmlUtil;
import com.xlongwei.light4j.util.JsonUtil;
import com.xlongwei.light4j.util.RedisConfig;
import com.xlongwei.light4j.util.SqlInsert;

import org.junit.Test;

import lombok.extern.slf4j.Slf4j;

/**
 * crawl test
 * @author xlongwei
 *
 */
@Slf4j
public class CrawlTest {
	private String dir = "../ourjs/train", cache = "html", html = "", url = "";
	
	@Test public void html() {
		url = "http://qq.ip138.com/train/";
		html = HtmlUtil.string(new File(dir, "train.html"));
//		url = "http://qq.ip138.com/train/anhui/";
//		html = HtmlUtil.string(new File(dir, "province.html"));
//		url = "http://qq.ip138.com/train/anhui/AnQing.htm";
//		html = HtmlUtil.string(new File(dir, "city.html"));
//		url = "http://qq.ip138.com/train/D5601.htm";
//		html = HtmlUtil.string(new File(dir, "line.html"));		
		RedisConfig.set(cache, url, html);
		log.info(html);
	}

	@Test public void crawl() {
		//所有crawls
		JSONArray crawls = JsonUtil.builder(true).add(
				JsonUtil.builder(false)
					.put("name", "train")
					.put("descr", "车次信息")
				.build()
			)
		.array();
		RedisConfig.set("crawler.crawl", crawls.toJSONString());
		log.info(RedisConfig.get("crawler.crawl"));
	}
	
	@Test public void step() {
		//每个crawl有多个steps
		JSONArray steps = JsonUtil.builder(true).add(
				JsonUtil.builder(false)
				.put("name", "train")
				.put("descr", "获取省份链接")
				.put("url", "http://qq.ip138.com/train/")
				.put("level", "1")
				.build()
			).add(
				JsonUtil.builder(false)
				.put("name", "province")
				.put("descr", "获取城市链接")
				.put("url", "http://qq.ip138.com/train/anhui/")
				.put("level", "2")
				.build()
			).add(
				JsonUtil.builder(false)
				.put("name", "city")
				.put("descr", "获取车次链接")
				.put("url", "http://qq.ip138.com/train/anhui/AnQing.htm")
				.put("level", "3")
				.build()
			).add(
				JsonUtil.builder(false)
				.put("name", "line")
				.put("descr", "获取站台信息")
				.put("url", "http://qq.ip138.com/train/D5601.htm")
				.put("level", "4")
				.build()
			)
		.array();
		RedisConfig.set("crawler.crawl.train", steps.toJSONString());		
	}
	
	@Test public void code() {
		//每个step有js-crawl-code
		html = HtmlUtil.string(new File(dir, "train-crawl.js"));
		RedisConfig.set("crawler.crawl.train.train", html);
		log.info(html);		
		html = HtmlUtil.string(new File(dir, "province-crawl.js"));
		RedisConfig.set("crawler.crawl.train.province", html);
		log.info(html);		
		html = HtmlUtil.string(new File(dir, "city-crawl.js"));
		RedisConfig.set("crawler.crawl.train.city", html);
		log.info(html);		
		html = HtmlUtil.string(new File(dir, "line-crawl.js"));
		RedisConfig.set("crawler.crawl.train.line", html);
		log.info(html);		
	}

	@Test public void bills() throws Exception {
		JSONObject json = JsonUtil.parseNew(FileUtil.readString(new File(".vscode/bills.json"), CharsetNames.UTF_8));
		JSONArray array = json.getJSONArray("contentlist");
		int size = array==null ? 0 : array.size();
		log.info("bills = {}", size);
		if(size>0){
			String[] fields = {"_id","apiCode","apiName","slaveId","slaveName","payTime","realPayMoney"};
			Map<String, Integer> maxLengthMap = new LinkedHashMap<>();
			SqlInsert insert = new SqlInsert("bills").addColumns(fields).batch(100).ignore(true);
			int len = fields.length;
			for(int i=0;i<size;i++){
				JSONObject obj = array.getJSONObject(i);
				String[] values=new String[len];
				for(int j=0;j<len;j++){
					String column=fields[j],value=obj.getString(column);
					values[j] = value;
					int valueLen = values[j].length();
					Integer maxLen=maxLengthMap.get(column);
					if(maxLen==null || valueLen>maxLen){
						maxLengthMap.put(column, valueLen);
					}
				}
				insert.addValues(values);
			}
			System.out.println(maxLengthMap);
			insert.sqls().forEach(System.out::println);
		}
	}
}
