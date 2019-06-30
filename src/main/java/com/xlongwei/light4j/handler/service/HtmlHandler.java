package com.xlongwei.light4j.handler.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import com.networknt.utility.StringUtils;
import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.ConfigUtil;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.HtmlUtil;
import com.xlongwei.light4j.util.IdWorker;
import com.xlongwei.light4j.util.JsonUtil;
import com.xlongwei.light4j.util.NumberUtil;
import com.xlongwei.light4j.util.RedisConfig;
import com.xlongwei.light4j.util.StringUtil;

import io.undertow.server.HttpServerExchange;
import lombok.extern.slf4j.Slf4j;

/**
 * html handler
 * @author xlongwei
 *
 */
@Slf4j
@SuppressWarnings("unchecked")
public class HtmlHandler extends AbstractHandler {

	public void charset(HttpServerExchange exchange) throws Exception {
		String charset = null;
		String url = HandlerUtil.getParam(exchange, "url");
		if(StringUtil.isUrl(url)) {
			charset = StringUtils.trimToEmpty(HtmlUtil.charset(url));
			HandlerUtil.setResp(exchange, StringUtil.params("charset", charset));
		}
		String bytes = HandlerUtil.getParam(exchange, "bytes");
		if(StringUtil.hasLength(bytes)) {
			byte[] bs = Base64.decodeBase64(bytes);
			charset = StringUtils.trimToEmpty(HtmlUtil.charset(bs));
			HandlerUtil.setResp(exchange, StringUtil.params("charset", charset));
		}
	}
	
	public void get(HttpServerExchange exchange) throws Exception {
		String url = HandlerUtil.getParam(exchange, "url");
		if(StringUtil.isUrl(url)) {
			boolean force = NumberUtil.parseBoolean(HandlerUtil.getParam(exchange, "force"), false);
			String html = RedisConfig.get(HtmlUtil.cache, url);
			if(force) {
				long seconds = RedisConfig.ttl(HtmlUtil.cache, url);
				log.info("force get html, url: {}, seconds: {}", url, seconds);
				int thirty = 30;
				if(Math.abs(seconds-RedisConfig.DEFAULT_SECONDS)>thirty) {
					html = null;
				}
			}
			if(html == null) {
				Map<String, String> headers = JsonUtil.parse(HandlerUtil.getParam(exchange, "headers"), Map.class);
				if(headers!=null && headers.size()>0) {
					List<Header> list = headers.entrySet().stream().map(entry -> new BasicHeader(entry.getKey(), entry.getValue())).collect(Collectors.toList());
					html = StringUtils.trimToEmpty(HtmlUtil.get(url, list.toArray(new Header[list.size()])));
				}else {
					html = StringUtils.trimToEmpty(HtmlUtil.get(url));
				}
				RedisConfig.set(HtmlUtil.cache, url, html);
			}
			HandlerUtil.setResp(exchange, StringUtil.params("html", html));
		}
	}
	
	public void post(HttpServerExchange exchange) throws Exception {
		String url = HandlerUtil.getParam(exchange, "url");
		if(StringUtil.isUrl(url)) {
			Map<String, String> headers = JsonUtil.parse(HandlerUtil.getParam(exchange, "headers"), Map.class);
			Map<String, String> params = JsonUtil.parse(HandlerUtil.getParam(exchange, "params"), Map.class);
			String html = StringUtils.trimToEmpty(HtmlUtil.post(url, headers, params));
			HandlerUtil.setResp(exchange, StringUtil.params("html", html));
		}
	}

	public void crawlConfig(HttpServerExchange exchange) throws Exception {
		String crawl = HandlerUtil.getParam(exchange, "crawl");
		String config = HandlerUtil.getParam(exchange, "config");
		boolean isDemo = StringUtil.hasLength(config) && crawl.startsWith("demo");
		if(StringUtil.isBlank(crawl) || (isDemo)) {
			//demo为演示配置，不允许外部使用
		}else {
			String key = "crawler.crawl.light4j-"+crawl;
			if(StringUtil.isBlank(config)) {
				config = RedisConfig.get(key);
			}else {
				RedisConfig.set(key, config);
			}
			if(!StringUtil.isBlank(config)) {
				HandlerUtil.setResp(exchange, StringUtil.params("crawl", crawl, "config", config));
			}
		}
	}
	
	public void crawlData(HttpServerExchange exchange) throws Exception {
		String crawl = HandlerUtil.getParam(exchange, "crawl");
		if(StringUtil.isBlank(crawl)) {
			return;
		}
		String key = "crawler.crawl.light4j-"+crawl;
		if(StringUtil.isBlank(RedisConfig.get(key)) || RedisConfig.ttl(RedisConfig.CACHE, key)>0){
			return;
		}
		String html = HandlerUtil.getParam(exchange, "html");
		String url = HandlerUtil.getParam(exchange, "url");
		if(StringUtil.isBlank(html)) {
			if(StringUtil.isUrl(url)) {
				final String finalUrl = url;
				html = RedisConfig.get(HtmlUtil.cache, url, () -> HtmlUtil.get(finalUrl));
			}else {
				return;
			}
		}else {
			url = ConfigUtil.FRONT_URL+"/"+IdWorker.getId();
			RedisConfig.set(HtmlUtil.cache, url, html);
		}
		String service = StringUtil.firstNotBlank(RedisConfig.get("crawler.crawl.ourjs"), "http://localhost:8055")+"/crawl.json";
		String resp = HtmlUtil.get(service+"?url=html:"+url+"&step=property:"+key);
		log.info("crawl data resp: {}", resp);
		Map<String, Object> map = new HashMap<>(4);
		map.put("crawl", crawl);
		map.put("url", url);
		map.put("data", JsonUtil.parseNew(resp));
		HandlerUtil.setResp(exchange, map);
	}
}
