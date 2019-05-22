package com.xlongwei.light4j.handler.weixin;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.xlongwei.light4j.util.HttpUtil;
import com.xlongwei.light4j.util.JsonUtil;
import com.xlongwei.light4j.util.RedisCache;
import com.xlongwei.light4j.util.RedisConfig;
import com.xlongwei.light4j.util.StringUtil;
import com.xlongwei.light4j.util.WeixinUtil.AbstractMessageHandler.AbstractTextHandler;

/**
 * youdao dict
 * @author xlongwei
 *
 */
public class YoudaoHandler extends AbstractTextHandler {
	private Pattern pattern = Pattern.compile("^翻译"+split+"([a-zA-Z]+|[\\u4E00-\\u9FA5]+)$");
	private static final String WORD = "[a-zA-Z]{3,}";
	private static final String ERROR_CODE = "errorCode";
	private static String KEY_FROM = "xlongwei", YOUDAO_API = "http://fanyi.youdao.com/openapi.do";
	private static String CACHE_WEIXIN = "weixin.translate", CACHE_YOUDAO = "youdao.translate";
	
	@Override
	public String handle(String content) {
		if(StringUtil.isBlank(content)) {
			return null;
		}
		String translate = null;
		Matcher matcher = pattern.matcher(content);
		if(matcher.matches()) {
			translate = matcher.group(1);
		} else if(content.matches(WORD)) {
			translate = content;
		}
		if(StringUtil.isBlank(translate)) {
			return null;
		}
		String cached = RedisCache.get(CACHE_WEIXIN, translate);
		if(!StringUtil.isBlank(cached)) {
			return cached;
		}
		cached = translate(translate);
		if(JsonUtil.getInt(cached, ERROR_CODE) == 0) {
			StringBuilder response = new StringBuilder();
			response.append(StringUtil.join(JsonUtil.getList(cached, "translation", String.class),null,null,","));
			response.append(", "+JsonUtil.get(cached, "basic.phonetic"));
			if(StringUtil.isIdentifier(translate)) {
				response.append(", (美) "+JsonUtil.get(cached, "basic.us-phonetic")+", (英) "+JsonUtil.get(cached, "basic.uk-phonetic"));
			}
			response.append("\n\n"+StringUtil.join(JsonUtil.getList(cached, "basic.explains", String.class), null, null, "\n"));
			List<String> webs = JsonUtil.getList(cached, "web", String.class);
			if(webs!=null && webs.size()>0) {
				response.append("\n");
				for(String web : webs) {
					response.append("\n"+JsonUtil.get(web, "key")+" "+StringUtil.join(JsonUtil.getList(web, "value", String.class), null, null, ","));
				}
			}
			cached = response.toString();
			RedisCache.set(CACHE_WEIXIN, translate, cached);
			return cached;
		}
		return null;
	}

	public static String translate(String text) {
		String key = RedisConfig.get("youdao.key");
		if(StringUtil.isBlank(key) || StringUtil.isBlank(KEY_FROM) || StringUtil.isBlank(text)) {
			return null;
		}
		String cached = RedisCache.get(CACHE_YOUDAO, text);
		if(!StringUtil.isBlank(cached)) {
			return cached;
		}
		cached = HttpUtil.post(YOUDAO_API, StringUtil.params("keyfrom",KEY_FROM,"key",key,"type","data","doctype","json","version","1.1","q",text));
		RedisCache.set(CACHE_YOUDAO, text, cached);
		return cached;
	}
}
