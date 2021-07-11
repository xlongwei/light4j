package com.xlongwei.light4j.handler.service;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.SimpleBindings;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.networknt.url.HttpURL;
import com.networknt.utility.StringUtils;
import com.networknt.utility.Tuple;
import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.ConfigUtil;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.HtmlUtil;
import com.xlongwei.light4j.util.IdWorker;
import com.xlongwei.light4j.util.JsonUtil;
import com.xlongwei.light4j.util.NumberUtil;
import com.xlongwei.light4j.util.QnObject;
import com.xlongwei.light4j.util.RedisConfig;
import com.xlongwei.light4j.util.StringUtil;
import com.xlongwei.light4j.util.QnObject.QnException;

import cn.hutool.core.map.MapUtil;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.cache.LRUCache;
import io.undertow.util.ObjectPool;
import io.undertow.util.PooledObject;
import io.undertow.util.SimpleObjectPool;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * html handler
 * <p>
 * <code>.settings/org.eclipse.jdt.core.prefs</code>
 * <code>org.eclipse.jdt.core.compiler.problem.forbiddenReference=ignore</code>
 * 
 * @author xlongwei
 *
 */
@Slf4j
@SuppressWarnings({ "unchecked" })
public class HtmlHandler extends AbstractHandler {
	private static final ObjectPool<ScriptEngine> POOL = new SimpleObjectPool<ScriptEngine>(
			NumberUtil.parseInt(RedisConfig.get(""), 6), () -> new NashornScriptEngineFactory().getScriptEngine("-strict", "--no-java", "--no-syntax-extensions"),
			ScriptEngine::getContext, ScriptEngine::getContext);
	private static final LRUCache<String, CompiledScript> SCRIPTS = new LRUCache<>(1000, -1);

	public void charset(HttpServerExchange exchange) throws Exception {
		String charset = null;
		String url = HandlerUtil.getParam(exchange, "url");
		if(StringUtil.isUrl(url)) {
			charset = StringUtils.trimToEmpty(HtmlUtil.charset(url));
			HandlerUtil.setResp(exchange, StringUtil.params("charset", charset));
			return;
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
			if(force && StringUtil.hasLength(html)) {
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
		boolean isDemo = !JsonUtil.isEmpty(config) && crawl.startsWith("demo");
		if(StringUtil.isBlank(crawl) || (isDemo)) {
			//demo为演示配置，不允许外部使用
		}else {
			String key = "crawler.crawl.light4j-"+crawl;
			String userName = HandlerUtil.getParam(exchange, "showapi_userName");
			boolean isClient = ConfigUtil.isClient(userName);
			if(isClient) {
				key = key.replace("light4j", userName);
			}
			if(JsonUtil.isEmpty(config)) {
				config = RedisConfig.get(key);
			}else {
				if(isClient==false) {
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
		String crawl = StringUtil.firstNotBlank(HandlerUtil.getParam(exchange, "crawl"), "temp");
		String key = "crawler.crawl.light4j-"+crawl;
		String userName = HandlerUtil.getParam(exchange, "showapi_userName");
		boolean isClient = ConfigUtil.isClient(userName), isTemp = "temp".equals(crawl);
		if(isClient) {
			key = key.replace("light4j", userName);
		}
		String bodyString = HandlerUtil.getBodyString(exchange);
		String html = HandlerUtil.getParam(exchange, "html");
		String url = HandlerUtil.getParam(exchange, "url");
		if(!StringUtil.isBlank(bodyString)) {
			if(bodyString.contains("<head>")) {
				if(JsonUtil.isEmpty(html)) {
					html = bodyString;
				}
			}else {
				if(isTemp) {
					RedisConfig.set(key, bodyString);
				}
			}
		}
		if(StringUtil.isBlank(RedisConfig.get(key))) {
			return;
		}
		if(JsonUtil.isEmpty(html)) {
			boolean isUrl = StringUtil.isUrl(url), isWhite = isUrl&&!isClient&&StringUtil.splitContains(RedisConfig.get("livecode.white-domains"), StringUtil.rootDomain(new HttpURL(url).getHost()));
			log.info("isUrl: {}, userName: {}, isClient: {}, isWhite: {}", isUrl, userName, isClient, isWhite);
			boolean clientOrWhite = isClient || isWhite;
			if(isUrl && clientOrWhite) {
				html = RedisConfig.get(HtmlUtil.cache, url);
				if(isTemp || JsonUtil.isEmpty(html)) {
					html = HtmlUtil.get(url);
					if(JsonUtil.isEmpty(html)) {
						return;
					}
					RedisConfig.set(HtmlUtil.cache, url, html);
				}
			}else {
				return;
			}
		}else {
			url = ConfigUtil.FRONT_URL+"/"+IdWorker.getId();
			RedisConfig.set(HtmlUtil.cache, url, html);
		}
		String service = StringUtil.firstNotBlank(RedisConfig.get("crawler.crawl.ourjs"), "http://localhost:8055")+"/crawl.json";
		String resp = HtmlUtil.get(service+"?url=html:"+url.replace("?", "%3F").replace("=", "%3D").replace("&", "%26")+"&step=property:"+key);
		log.info("crawl data resp: {}", resp);
		Map<String, Object> map = new HashMap<>(4);
		map.put("crawl", crawl);
		map.put("url", url);
		map.put("data", JsonUtil.parseNew(resp));
		HandlerUtil.setResp(exchange, map);
	}
	
	public void jsConfig(HttpServerExchange exchange) throws Exception {
		String userName = HandlerUtil.getParam(exchange, "showapi_userName");
		boolean isClient = ConfigUtil.isClient(userName);
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
				data = RedisConfig.get("js."+(isClient==false?"":userName+".")+datakey);
				map.put("data", StringUtil.hasLength(data) ? data : "no data config");
			}
			if(StringUtil.hasLength(jskey)) {
				js = RedisConfig.get("js."+(isClient==false?"":userName+".")+jskey);
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
					datakey = "js."+(isClient==false?"":userName+".")+datakey;
					RedisConfig.set(datakey, data);
				}else {
					map.put("error", "data must be json object or json array");
				}
			}
			if(StringUtil.hasLength(js)) {
				jskey = StringUtil.firstNotBlank(jskey, String.valueOf(IdWorker.getId()));
				map.put("jskey", jskey);
				jskey = "js."+(isClient==false?"":userName+".")+jskey;
				RedisConfig.set(jskey, js);
				SCRIPTS.remove(jskey);
				log.info("jsConfig remove compiled script, jskey: {}", jskey);
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
			boolean isClient = ConfigUtil.isClient(userName);
			String clientName = isClient==false?"":userName+".";
			//优先取redis配置，其次取原值，userName独立配置
			data = data.charAt(0)!='{' && data.charAt(0)!='[' ? RedisConfig.get("js."+clientName+data) : data;
			String jskey = "js."+clientName+js;
			String jsConfig = RedisConfig.get(jskey);
			if(StringUtil.hasLength(jsConfig)) {
				js = jsConfig;
			}else {
				jskey = null;
			}
			if(StringUtil.hasLength(data) && StringUtil.hasLength(js)) {
				Tuple<Boolean, String> result = jsEval(data, js, jskey);
				HandlerUtil.setResp(exchange, StringUtil.params(result.first ? "result" : "error", result.second));
			}else {
				HandlerUtil.setResp(exchange, StringUtil.params("error", "empty data && js"));
			}
		}
	}
	
	public void jsEvals(HttpServerExchange exchange) throws Exception {
		String jskeys = StringUtil.firstNotBlank(HandlerUtil.getParam(exchange, "jskeys"), HandlerUtil.getParam(exchange, "jskey"));
		String datakeys = StringUtil.firstNotBlank(HandlerUtil.getParam(exchange, "datakeys"), HandlerUtil.getParam(exchange, "datakey"));
		String data = HandlerUtil.getBodyString(exchange);
		if(StringUtil.isBlank(jskeys) || (StringUtil.isBlank(datakeys) && StringUtil.isBlank(data))) {
			HandlerUtil.setResp(exchange, StringUtil.params("error", "empty datakeys or jskeys"));
		}else {
			String userName = HandlerUtil.getParam(exchange, "showapi_userName");
			boolean isClient = ConfigUtil.isClient(userName);
			String clientName = isClient==false?"":userName+".", js = null;
			List<Map<String, String>> results = new LinkedList<>();
			for(String jskey : jskeys.split("[,]")) {
				jskey = "js." + clientName + jskey;
				js = RedisConfig.get(jskey);
				if(StringUtil.isBlank(js)) {
					results.add(StringUtil.params("jskey", jskey, "error", "empty js"));
				}else if(StringUtil.isBlank(datakeys)) {
					Tuple<Boolean, String> tuple = jsEval(data, js, jskey);
					results.add(StringUtil.params("jskey", jskey, tuple.first ? "result" : "error", tuple.second));
				}else {
					for(String datakey : datakeys.split("[,]")) {
						datakey = "js." + clientName + datakey;
						data = RedisConfig.get(datakey);
						if(StringUtil.isBlank(data)) {
							results.add(StringUtil.params("jskey", jskey, "datakey", datakey, "error", "empty data"));
						}else {
							Tuple<Boolean, String> tuple = jsEval(data, js, jskey);
							results.add(StringUtil.params("jskey", jskey, "datakey", datakey, tuple.first ? "result" : "error", tuple.second));
						}
					}
				}
			}
			HandlerUtil.setResp(exchange, MapUtil.of("results", results));
		}
	}
	
	public void jsQn(HttpServerExchange exchange) throws Exception {
		String qn = StringUtil.firstNotBlank(HandlerUtil.getParam(exchange, "qn"), HandlerUtil.getBodyString(exchange));
		if(StringUtil.isBlank(qn)) {
			return;
		}
		try {
			String js = NumberUtil.parseBoolean(HandlerUtil.getParam(exchange, "isCondition"), false) ? QnObject.fromQc(qn).toJs() : QnObject.fromQn(qn).toJs();
			HandlerUtil.setResp(exchange, StringUtil.params("js", js));
		}catch(QnException e) {
			HandlerUtil.setResp(exchange, StringUtil.params("pos", String.valueOf(e.getPos()), "error", e.getMessage()));
		}
	}
	
	public void jsQnEval(HttpServerExchange exchange) throws Exception {
		String data = HandlerUtil.getParam(exchange, "data"), datakey = HandlerUtil.getParam(exchange, "datakey");
		if(StringUtil.isBlank(data) && StringUtil.isBlank(datakey)) {
			return;
		}
		if(StringUtil.isBlank(data) || (data.charAt(0)!='{' && data.charAt(0)!='[')) {
			String userName = HandlerUtil.getParam(exchange, "showapi_userName");
			boolean isClient = ConfigUtil.isClient(userName);
			String clientName = isClient==false?"":userName+".";
			datakey = "js." + clientName + StringUtil.firstNotBlank(data, datakey);
			data = RedisConfig.get(datakey);
		}
		String qn = StringUtil.firstNotBlank(HandlerUtil.getParam(exchange, "qn"), HandlerUtil.getBodyString(exchange));
		if(StringUtil.isBlank(qn) || StringUtil.isBlank(data)) {
			return;
		}
		try {
			String js = NumberUtil.parseBoolean(HandlerUtil.getParam(exchange, "isCondition"), false) ? QnObject.fromQc(qn).toJs() : QnObject.fromQn(qn).toJs();
			Tuple<Boolean, String> result = jsEval(data, js, null);
			HandlerUtil.setResp(exchange, StringUtil.params(result.first ? "result" : "error", result.second));
		}catch(QnException e) {
			HandlerUtil.setResp(exchange, StringUtil.params("pos", String.valueOf(e.getPos()), "error", e.getMessage()));
		}
	}

	private Tuple<Boolean, String> jsEval(String data, String js, String jskey) {
		log.info("jsEval data: {}, jskey: {}, js: \n{}", data, jskey, js);
		JSONObject dataObj = StringUtil.isBlank(data)||'{'!=data.charAt(0) ? null : JsonUtil.parse(data);
		JSONArray dataArray = StringUtil.isBlank(data)||'['!=data.charAt(0) ? null : JsonUtil.parseArray(data);
		if(dataObj!=null || dataArray!=null) {
			Bindings bindings = new SimpleBindings();
			bindings.put("data", dataObj!=null ? dataObj : dataArray);
			Object result = null;
			if(jskey!=null) {
				CompiledScript script = SCRIPTS.get(jskey);
				if(script!=null) {
					try{
						result = script.eval(bindings);
						return new Tuple<>(Boolean.TRUE, String.valueOf(result));
					}catch(Exception e) {
						log.info("fail to eval js, ex: {}", e.getMessage());
						return new Tuple<>(Boolean.FALSE, e.getMessage());
					}
				}
			}
			try(PooledObject<ScriptEngine> pooledObject = POOL.allocate()){
				ScriptEngine se = pooledObject.getObject();
				result = se.eval(js, bindings);
				if(jskey!=null) {
					CompiledScript script = ((Compilable)se).compile(js);
					SCRIPTS.add(jskey, script);
					log.info("jsEval save compiled script, jskey: {}", jskey);
				}
				return new Tuple<>(Boolean.TRUE, String.valueOf(result));
			}catch(Exception e) {
				log.info("fail to eval js, ex: {}", e.getMessage());
				return new Tuple<>(Boolean.FALSE, e.getMessage());
			}
		}else {
			return new Tuple<>(Boolean.FALSE, "bad data config");
		}
	}
}
