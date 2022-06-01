package com.xlongwei.light4j.handler.service;

import com.alibaba.fastjson.JSONObject;
import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.handler.weixin.SubscribeHandler;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.JsonUtil;
import com.xlongwei.light4j.util.RedisConfig;
import com.xlongwei.light4j.util.StringUtil;
import com.xlongwei.light4j.util.WeixinUtil;
import com.xlongwei.light4j.util.WeixinUtil.AbstractMessage;
import com.xlongwei.light4j.util.WeixinUtil.AbstractMessage.TextMessage;

import apijson.JSON;
import cn.hutool.http.HttpUtil;
import io.undertow.server.HttpServerExchange;
import lombok.extern.slf4j.Slf4j;

/**
 * weixin handler
 * @author xlongwei
 *
 */
@Slf4j
public class WeixinHandler extends AbstractHandler {

	public void chat(HttpServerExchange exchange) throws Exception {
		String text = HandlerUtil.getParam(exchange, "text");
		if(StringUtil.isBlank(text)) {
			return;
		}
		
		text = chat(HandlerUtil.getParam(exchange, "openid"), text);
		if(!StringUtil.isBlank(text)) {
			HandlerUtil.setResp(exchange, StringUtil.params("text", text));
		}
	}

	public static String chat(String openid, String text) {
		TextMessage textMsg = new TextMessage();
		textMsg.setContent(text);
		textMsg.setFromUserName(openid);
		AbstractMessage dispatch = WeixinUtil.dispatch(textMsg);
		if(dispatch!=null && dispatch instanceof TextMessage) {
			log.info("weixin.chat with {}", text);
			text = ((TextMessage)dispatch).getContent();
			log.info("weixin.chat resp {}", text);
			return text;
		}else{
			return null;
		}
	}
	
	public void notify(HttpServerExchange exchange) throws Exception {
		JSONObject resp = notify(HandlerUtil.getParam(exchange, "openid"), HandlerUtil.getParam(exchange, "text"), HandlerUtil.getParam(exchange, "chat"));
		if(resp != null) {
			HandlerUtil.setResp(exchange, StringUtil.params("text", StringUtil.firstNotBlank(JsonUtil.get(resp, "errmsg"), "失败")));
		}
	}

	public static JSONObject notify(String openid, String text, String chat) {
		if(StringUtil.isBlank(openid)) {
			return null;
		}
		String subscribe = RedisConfig.get(RedisConfig.CACHE, SubscribeHandler.WEIXIN_SUBSCRIBE+openid);
		log.info("weixin.notify {} subscribe at {}", openid, subscribe);
		// if(StringUtil.isBlank(subscribe)) {
		// 	return null;
		// }
		if(StringUtil.isBlank(text) && !StringUtil.isBlank(chat)) {
			text = chat(openid, chat);
		}
		if(StringUtil.isBlank(text)) {
			return null;
		}
		JSONObject send = JsonUtil.builder(false)
				.put("touser", openid)
				.put("template_id", "WEwOHqLkqTcRJlJSDi2okAU2gRUpgqW6Ah1soe0r6BI")
				.put("url", "http://u5335.showapi.com/")
				.putJSON("data")
					.putJSON("notice")
						.put("value", text)
				.top().json();
		JSONObject resp = WeixinUtil.templateSend(WeixinUtil.accessToken(WeixinUtil.appidTest, StringUtil.firstNotBlank(RedisConfig.get(WeixinUtil.cache, "appsecret."+WeixinUtil.appidTest), "d4624c36b6795d1d99dcf0547af5443d")), send);
		return resp;
	}

	public void token(HttpServerExchange exchange) throws Exception {
		String appid = HandlerUtil.getParam(exchange, "appid"), appsecret = HandlerUtil.getParam(exchange, "appsecret");
		if(StringUtil.isBlank(appid)) {
			return;
		}
		if(StringUtil.isBlank(appsecret)) {
			appsecret = RedisConfig.get(WeixinUtil.cache, "appsecret."+appid);
			if(StringUtil.isBlank(appsecret)) {
				return;
			}
		}else {
			RedisConfig.set(WeixinUtil.cache, "appsecret."+appid, appsecret);
		}
		String refresh = HandlerUtil.getParam(exchange, "refresh");
		if("force".equals(refresh)) {
			RedisConfig.delete(WeixinUtil.cache, WeixinUtil.accessTokenKey+"."+appid);
		}
		String accessToken = WeixinUtil.accessToken(appid, appsecret);
		if(StringUtil.hasLength(accessToken)) {
			HandlerUtil.setResp(exchange, StringUtil.params("access_token", accessToken));
		}
	}
	
	public void encrypt(HttpServerExchange exchange) throws Exception {
		String appid = HandlerUtil.getParam(exchange, "appid");
		if(StringUtil.isBlank(appid)) {
			return;
		}
		String token = HandlerUtil.getParam(exchange, "token"), encodingAesKey = HandlerUtil.getParam(exchange, "encodingAesKey");
		if(StringUtil.hasLength(token) && StringUtil.hasLength(encodingAesKey)) {
			WeixinUtil.configAES(appid, token, encodingAesKey);
		}
		String timestamp = HandlerUtil.getParam(exchange, "timestamp"), nonce = HandlerUtil.getParam(exchange, "nonce");
		String replyMsg = HandlerUtil.getParam(exchange, "replyMsg");
		if(!StringUtil.isBlank(replyMsg)) {
			String encrypt = WeixinUtil.encrypt(appid, replyMsg, timestamp, nonce);
			if(!StringUtil.isBlank(encrypt)) {
				HandlerUtil.setResp(exchange, StringUtil.params("encrypt", encrypt));
			}
		}
	}
	
	public void decrypt(HttpServerExchange exchange) throws Exception {
		String appid = HandlerUtil.getParam(exchange, "appid");
		if(StringUtil.isBlank(appid)) {
			return;
		}
		String token = HandlerUtil.getParam(exchange, "token"), encodingAesKey = HandlerUtil.getParam(exchange, "encodingAesKey");
		if(StringUtil.hasLength(token) && StringUtil.hasLength(encodingAesKey)) {
			WeixinUtil.configAES(appid, token, encodingAesKey);
		}
		String timestamp = HandlerUtil.getParam(exchange, "timestamp"), nonce = HandlerUtil.getParam(exchange, "nonce");
		String msgSignature = HandlerUtil.getParam(exchange, "msgSignature"), fromXML = HandlerUtil.getParam(exchange, "fromXML");
		if(StringUtil.hasLength(msgSignature) && StringUtil.hasLength(fromXML)) {
			String decrypt = WeixinUtil.decrypt(appid, msgSignature, timestamp, nonce, fromXML);
			if(StringUtil.hasLength(decrypt)) {
				HandlerUtil.setResp(exchange, StringUtil.params("decrypt", decrypt));
			}
		}
	}

	public void user(HttpServerExchange exchange) throws Exception {
		String token = token(HandlerUtil.getParam(exchange, "token"), HandlerUtil.getParam(exchange, "appid"));
		if(StringUtil.isBlank(token)) {
			return;
		}
		String openid = HandlerUtil.getParam(exchange, "openid"), json = null;
		if(StringUtil.isBlank(openid)) {
			String next_openid = HandlerUtil.getParam(exchange, "next_openid");
			json = HttpUtil.get(WeixinUtil.service+"user/get?access_token="+token+"&next_openid="+next_openid);
		}else{
			json = HttpUtil.get(WeixinUtil.service+"user/info?access_token="+token+"&openid="+openid+"&lang=zh_CN");
		}
		HandlerUtil.setResp(exchange, JSON.parseObject(json));
	}

	public void menu(HttpServerExchange exchange) throws Exception {
		String appid = HandlerUtil.getParam(exchange, "appid");
		String token = token(HandlerUtil.getParam(exchange, "token"), appid);
		if(StringUtil.isBlank(token)) {
			return;
		}
		String menu = HandlerUtil.getParam(exchange, "menu"), json = null;
		if(StringUtil.isBlank(menu)) {
			json = HttpUtil.get(WeixinUtil.service+"menu/get?access_token="+token);
			if(!StringUtil.isBlank(appid) && 40001==JsonUtil.getInt(json, "errcode")){
				RedisConfig.delete(WeixinUtil.cache, WeixinUtil.accessTokenKey+"."+appid);
			}
		}else{
			json = HttpUtil.post(WeixinUtil.service+"menu/create?access_token="+token, menu);
		}
		HandlerUtil.setResp(exchange, JSON.parseObject(json));
	}

	private String token(String token, String appid) {
		if(StringUtil.isBlank(token) && !StringUtil.isBlank(appid)) {
			token = RedisConfig.get(WeixinUtil.cache, WeixinUtil.accessTokenKey+"."+appid);
			if(StringUtil.isBlank(token)){
				String appsecret = RedisConfig.get(WeixinUtil.cache, "appsecret."+appid);
				if(!StringUtil.isBlank(appsecret)) {
					token = WeixinUtil.accessToken(appid, appsecret);
				}
			}
		}
		return token;
	}
}
