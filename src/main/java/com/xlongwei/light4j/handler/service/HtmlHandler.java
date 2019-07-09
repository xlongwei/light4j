package com.xlongwei.light4j.handler.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
import io.undertow.util.ObjectPool;
import io.undertow.util.PooledObject;
import io.undertow.util.SimpleObjectPool;
import lombok.extern.slf4j.Slf4j;

/**
 * html handler
 * @author xlongwei
 *
 */
@Slf4j
@SuppressWarnings("unchecked")
public class HtmlHandler extends AbstractHandler {
	private static final ObjectPool<ScriptEngine> POOL = new SimpleObjectPool<ScriptEngine>(
			NumberUtil.parseInt(RedisConfig.get(""), 6), () -> new ScriptEngineManager().getEngineByName("js"),
			ScriptEngine::getContext, ScriptEngine::getContext);
	private static final Map<String, CompiledScript> SCRIPTS = new ConcurrentHashMap<>();

	public void charset(HttpServerExchange exchange) throws Exception {
		String charset = null;
		String url = HandlerUtil.getParam(exchange, "url");
		if(StringUtil.isUrl(url)) {
			charset = StringUtils.trimToEmpty(HtmlUtil.charset(url));
			HandlerUtil.setResp(exchange, StringUtil.params("charset", charset));
		}
		String bytes = HandlerUtil.getParamOrBody(exchange, "bytes");
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
		String config = HandlerUtil.getParamOrBody(exchange, "config");
		boolean isDemo = StringUtil.hasLength(config) && crawl.startsWith("demo");
		if(StringUtil.isBlank(crawl) || (isDemo)) {
			//demo为演示配置，不允许外部使用
		}else {
			String key = "crawler.crawl.light4j-"+crawl;
			String userName = HandlerUtil.getParam(exchange, "showapi_userName");
			if(!StringUtil.isBlank(userName)) {
				key = key.replace("light4j", userName);
			}
			if(StringUtil.isBlank(config)) {
				config = RedisConfig.get(key);
			}else {
				if(StringUtil.isBlank(userName)) {
					RedisConfig.set(key, config);
				}else {
					RedisConfig.persist(key, config);
				}
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
		String userName = HandlerUtil.getParam(exchange, "showapi_userName");
		if(!StringUtil.isBlank(userName)) {
			key = key.replace("light4j", userName);
		}
		if(StringUtil.isBlank(RedisConfig.get(key)) || RedisConfig.ttl(RedisConfig.CACHE, key)>0){
			return;
		}
		String html = HandlerUtil.getParamOrBody(exchange, "html");
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
	
	public void jsConfig(HttpServerExchange exchange) throws Exception {
		String userName = HandlerUtil.getParam(exchange, "showapi_userName");
		String data = HandlerUtil.getParam(exchange, "data"), datakey = HandlerUtil.getParam(exchange, "datakey");
		String js = HandlerUtil.getParam(exchange, "js"), jskey = HandlerUtil.getParam(exchange, "jskey");
		if(StringUtil.isBlank(data) && StringUtil.hasLength(datakey)) {
			data = HandlerUtil.getBodyString(exchange);
		}else if(StringUtil.isBlank(js) && StringUtil.hasLength(jskey)) {
			js = HandlerUtil.getBodyString(exchange);
		}
		if(StringUtil.isBlank(data) && StringUtil.isBlank(js)) {
			if(StringUtil.isBlank(datakey) && StringUtil.isBlank(jskey)) {
				return;
			}
			Map<String, String> map = new HashMap<>(2);
			if(StringUtil.hasLength(datakey)) {
				data = RedisConfig.get("js."+(StringUtil.isBlank(userName)?"":userName+".")+datakey);
				map.put("data", StringUtil.hasLength(data) ? data : "no data config");
			}
			if(StringUtil.hasLength(jskey)) {
				js = RedisConfig.get("js."+(StringUtil.isBlank(userName)?"":userName+".")+jskey);
				map.put("js", StringUtil.hasLength(js) ? js : "no js config");
			}
			HandlerUtil.setResp(exchange, map);
		}else {
			Map<String, String> map = new HashMap<>(2);
			if(StringUtil.hasLength(data)) {
				boolean valid = JsonUtil.parse(data)!=null || JsonUtil.parseArray(data)!=null;
				if(valid) {
					datakey = StringUtil.firstNotBlank(datakey, String.valueOf(IdWorker.getId()));
					map.put("datakey", datakey);
					datakey = "js."+(StringUtil.isBlank(userName)?"":userName+".")+datakey;
					RedisConfig.set(datakey, data);
				}else {
					map.put("error", "data must be json object or json array");
				}
			}
			if(StringUtil.hasLength(js)) {
				jskey = StringUtil.firstNotBlank(jskey, String.valueOf(IdWorker.getId()));
				map.put("jskey", jskey);
				jskey = "js."+(StringUtil.isBlank(userName)?"":userName+".")+jskey;
				RedisConfig.set(jskey, js);
				SCRIPTS.remove(jskey);
			}
			HandlerUtil.setResp(exchange, map);
		}
	}
	
	public void jsEval(HttpServerExchange exchange) throws Exception {
		String data = HandlerUtil.getParam(exchange, "data");
		String js = HandlerUtil.getParam(exchange, "js");
		//可以向?data=datakey提交js正文，也可以向?js=jskey提交data正文
		if(StringUtil.isBlank(data) && StringUtil.hasLength(js)) {
			data = HandlerUtil.getBodyString(exchange);
		}else if(StringUtil.isBlank(js) && StringUtil.hasLength(data)) {
			js = HandlerUtil.getBodyString(exchange);
		}
		if(StringUtil.hasLength(data) && StringUtil.hasLength(js)) {
			String userName = HandlerUtil.getParam(exchange, "showapi_userName");
			//优先取redis配置，其次取原值，userName独立配置
			data = StringUtil.firstNotBlank(RedisConfig.get("js."+(StringUtil.isBlank(userName)?"":userName+".")+data), data);
			String jskey = "js."+(StringUtil.isBlank(userName)?"":userName+".")+js;
			String jsConfig = RedisConfig.get(jskey);
			if(StringUtil.hasLength(jsConfig)) {
				js = jsConfig;
			}else {
				jskey = null;
			}
			if(StringUtil.hasLength(data) && StringUtil.hasLength(js)) {
				log.info("jsEval data: {}, js: \n{}", data, js);
				JSONObject dataObj = JsonUtil.parse(data);
				JSONArray dataArray = JsonUtil.parseArray(data);
				if(dataObj!=null || dataArray!=null) {
					Bindings bindings = new SimpleBindings();
					bindings.put("data", dataObj!=null ? dataObj : dataArray);
					Object result = null;
					if(jskey!=null) {
						CompiledScript script = SCRIPTS.get(jskey);
						if(script!=null) {
							try{
								result = script.eval(bindings);
								HandlerUtil.setResp(exchange, Collections.singletonMap("result", result));
							}catch(Exception e) {
								log.info("fail to eval js, ex: {}", e.getMessage());
								HandlerUtil.setResp(exchange, StringUtil.params("error", e.getMessage()));
							}
							return;
						}
					}
					try(PooledObject<ScriptEngine> pooledObject = POOL.allocate()){
						ScriptEngine se = pooledObject.getObject();
						result = se.eval(js, bindings);
						HandlerUtil.setResp(exchange, Collections.singletonMap("result", result));
						if(jskey!=null) {
							CompiledScript script = ((Compilable)se).compile(js);
							SCRIPTS.put(jskey, script);
						}
					}catch(Exception e) {
						log.info("fail to eval js, ex: {}", e.getMessage());
						HandlerUtil.setResp(exchange, StringUtil.params("error", e.getMessage()));
					}
				}else {
					HandlerUtil.setResp(exchange, StringUtil.params("error", "bad data config"));
				}
			}else {
				HandlerUtil.setResp(exchange, StringUtil.params("error", "empty data && js"));
			}
		}
	}
}
