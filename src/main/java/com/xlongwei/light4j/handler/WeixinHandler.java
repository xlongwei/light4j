package com.xlongwei.light4j.handler;

import java.util.List;

import com.networknt.handler.LightHttpHandler;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.StringUtil;
import com.xlongwei.light4j.util.WeixinUtil;
import com.xlongwei.light4j.util.WeixinUtil.AbstractMessage;
import com.xlongwei.light4j.util.WeixinUtil.AbstractMessageHandler;

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
		String weixin = getClass().getPackage().getName()+".weixin";
		List<AbstractMessageHandler> list = HandlerUtil.scanHandlers(AbstractMessageHandler.class, weixin);
		for(AbstractMessageHandler handler : list) {
			WeixinUtil.register(handler);
		}
	}

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }
		HandlerUtil.parseBody(exchange);
		String echostr = HandlerUtil.getParam(exchange, "echostr");
		if(!StringUtil.isBlank(echostr)) {
			String signature = HandlerUtil.getParam(exchange, "signature");
			String timestamp = HandlerUtil.getParam(exchange, "timestamp");
			String nonce = HandlerUtil.getParam(exchange, "nonce");
			String token = HandlerUtil.getParam(exchange, "token");
			String response = WeixinUtil.checkSignature(signature, timestamp, nonce, token) ? echostr : "";
			exchange.getResponseSender().send(response);
		}else {
			String xml = HandlerUtil.getParam(exchange, HandlerUtil.BODYSTRING);
			String timestamp = HandlerUtil.getParam(exchange, "timestamp");
			String nonce = HandlerUtil.getParam(exchange, "nonce");
			boolean aes = "aes".equals(HandlerUtil.getParam(exchange, "encrypt_type"));
			log.info(xml);
			if(aes) {
				String msgSignature = HandlerUtil.getParam(exchange, "msg_signature");
				String decrypt = WeixinUtil.decrypt(WeixinUtil.appid, msgSignature, timestamp, nonce, xml);
				logger.info("decrypt: "+decrypt);
				xml = decrypt;
			}
			AbstractMessage msg = StringUtil.isBlank(xml) ? null : AbstractMessage.fromXML(xml);
			msg = msg == null ? null : WeixinUtil.dispatch(msg);
			xml = msg !=null ? msg.toXML() : "";
			logger.info(xml);
			if(aes && !StringUtil.isBlank(xml)) {
				String encrypt = WeixinUtil.encrypt(WeixinUtil.appid, xml, timestamp, nonce);
				logger.info("encrypt: "+encrypt);
				xml = encrypt;
			}
			exchange.getResponseSender().send(xml);
		}
	}
	
}
