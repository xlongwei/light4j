package com.xlongwei.light4j.handler.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import com.networknt.utility.StringUtils;
import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.HtmlUtil;
import com.xlongwei.light4j.util.JsonUtil;
import com.xlongwei.light4j.util.NumberUtil;
import com.xlongwei.light4j.util.RedisCache;
import com.xlongwei.light4j.util.StringUtil;

import io.undertow.server.HttpServerExchange;

/**
 * html handler
 * @author xlongwei
 *
 */
@SuppressWarnings("unchecked")
public class HtmlHandler extends AbstractHandler {
	
	public static final String CACHE = "html.url";

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
			String html = RedisCache.get(CACHE, url);
			if(force) {
				long seconds = RedisCache.ttl(CACHE, url);
				if(seconds<0 || Math.abs(seconds-RedisCache.DEFAULT_SECONDS)>90) {
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
				RedisCache.set(CACHE, url, html);
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

}
