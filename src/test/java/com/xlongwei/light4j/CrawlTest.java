package com.xlongwei.light4j;

import java.io.File;

import org.junit.Test;

import com.alibaba.fastjson.JSONArray;
import com.xlongwei.light4j.util.HtmlUtil;
import com.xlongwei.light4j.util.JsonUtil;
import com.xlongwei.light4j.util.RedisConfig;

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
}
