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
		
		TextMessage textMsg = new TextMessage();
		textMsg.setContent(text);
		AbstractMessage dispatch = WeixinUtil.dispatch(textMsg);
		if(dispatch!=null && dispatch instanceof TextMessage) {
			text = ((TextMessage)dispatch).getContent();
			HandlerUtil.setResp(exchange, StringUtil.params("text", text));
		}
	}
	
	public void notify(HttpServerExchange exchange) throws Exception {
		String text = HandlerUtil.getParam(exchange, "text");
		String openid = HandlerUtil.getParam(exchange, "openid");
		if(StringUtil.isBlank(text) || StringUtil.isBlank(openid)) {
			return;
		}
		
		String subscribe = RedisConfig.get(RedisConfig.CACHE, SubscribeHandler.WEIXIN_SUBSCRIBE+openid);
		log.info("weixin.notify {} subscribe at {}", openid, subscribe);
		if(StringUtil.isBlank(subscribe)) {
			return;
		}
		
		JSONObject send = JsonUtil.builder(false)
				.put("touser", openid)
				.put("template_id", "WEwOHqLkqTcRJlJSDi2okAU2gRUpgqW6Ah1soe0r6BI")
				.put("url", "http://u5335.showapi.com/")
				.putJSON("data")
					.putJSON("notice")
						.put("value", text)
				.top().json();
		JSONObject resp = WeixinUtil.templateSend(WeixinUtil.accessToken(WeixinUtil.appidTest, WeixinUtil.appsecretTest), send);
		HandlerUtil.setResp(exchange, StringUtil.params("text", StringUtil.firstNotBlank(JsonUtil.get(resp, "errmsg"), "失败")));
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

}
