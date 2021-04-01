package com.xlongwei.light4j.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.networknt.handler.LightHttpHandler;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.StringUtil;
import com.xlongwei.light4j.util.WeixinUtil;
import com.xlongwei.light4j.util.WeixinUtil.AbstractMessage;
import com.xlongwei.light4j.util.WeixinUtil.AbstractMessageHandler;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;
import io.undertow.server.HttpServerExchange;
import lombok.extern.slf4j.Slf4j;

/**
 * weixin handler
 * @author xlongwei
 *
 */
@Slf4j
@SuppressWarnings({"rawtypes"})
public class WeixinHandler implements LightHttpHandler {
	
	public WeixinHandler() {
		String pkg = getClass().getPackage().getName()+".weixin";
		List<Class<?>> list = new ArrayList<>(ClassUtil.scanPackageBySuper(pkg, AbstractMessageHandler.class));
		Collections.sort(list, (a,b)->{ return a.getName().compareTo(b.getName()); });
		for(Class<?> clazz : list) {
			AbstractMessageHandler handler = (AbstractMessageHandler)ReflectUtil.newInstanceIfPossible(clazz);
			WeixinUtil.register(handler);
		}
	}

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		HandlerUtil.parseBody(exchange);
		String echostr = HandlerUtil.getParam(exchange, "echostr");
		String timestamp = HandlerUtil.getParam(exchange, "timestamp");
		String nonce = HandlerUtil.getParam(exchange, "nonce");
		if(!StringUtil.isBlank(echostr)) {
			String signature = HandlerUtil.getParam(exchange, "signature");
			String token = HandlerUtil.getParam(exchange, "token");
			String response = WeixinUtil.checkSignature(signature, timestamp, nonce, token) ? echostr : "";
			exchange.getResponseSender().send(response);
			log.info("echostr: {}, timestamp: {}, nonce: {}, signature: {}, token: {}", echostr, timestamp, nonce, signature, token);
		}else {
			String xml = HandlerUtil.getBodyString(exchange);
			boolean aes = "aes".equals(HandlerUtil.getParam(exchange, "encrypt_type"));
			log.info("timestamp: {}, nonce: {}, aes: {}", timestamp, nonce, aes);
			if(aes) {
				String msgSignature = HandlerUtil.getParam(exchange, "msg_signature");
				String decrypt = WeixinUtil.decrypt(WeixinUtil.appid, msgSignature, timestamp, nonce, xml);
				log.info("msg_signature: {}, decrypt: {}", msgSignature, decrypt);
				xml = decrypt;
			}
			AbstractMessage msg = StringUtil.isBlank(xml) ? null : AbstractMessage.fromXML(xml);
			msg = msg == null ? null : WeixinUtil.dispatch(msg);
			xml = msg !=null ? msg.toXML() : "";
			log.info("response: {}", xml);
			if(aes && !StringUtil.isBlank(xml)) {
				String encrypt = WeixinUtil.encrypt(WeixinUtil.appid, xml, timestamp, nonce);
				log.info("encrypt: {}", encrypt);
				xml = encrypt;
			}
			exchange.getResponseSender().send(xml);
		}
	}
	
}
