package com.xlongwei.light4j.handler;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.networknt.handler.LightHttpHandler;
import com.xlongwei.light4j.util.FileUtil.Charsets;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.IdWorker.SystemClock;
import com.xlongwei.light4j.util.NumberUtil;
import com.xlongwei.light4j.util.RedisConfig;
import com.xlongwei.light4j.util.StringUtil;
import com.xlongwei.light4j.util.WeixinUtil;
import com.xlongwei.light4j.util.WeixinUtil.Event;
import com.xlongwei.light4j.util.WeixinUtil.Event.ClickEvent;
import com.xlongwei.light4j.util.WeixinUtil.Message;
import com.xlongwei.light4j.util.WeixinUtil.Message.TextMessage;

import io.github.lukehutch.fastclasspathscanner.utils.ReflectionUtils;
import io.undertow.server.HttpServerExchange;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings({"rawtypes", "unchecked"})
public class WeixinHandler implements LightHttpHandler {
	private Map<Class, List<MessageHandler>> handlers = new HashMap<>();
	private TextMessage help = new TextMessage();
	
	public WeixinHandler() {
		List<MessageHandler> list = HandlerUtil.scanHandlers(MessageHandler.class, getClass().getPackage().getName()+".weixin");
		for(MessageHandler handler : list) {
			Class type = getMessageType(handler.getClass());
			List<MessageHandler> msgHandlers = handlers.get(type);
			if(msgHandlers==null) {
				handlers.put(type, msgHandlers = new ArrayList<>());
			}
			msgHandlers.add(handler);
			log.info("load {} => {}", type.getSimpleName(), handler.getClass().getName());
		}
		help.setContent(RedisConfig.get("weixin.help"));
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
			Message msg = Message.fromXML(xml);
			msg = msg == null ? null : dispatch(msg);
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
	
	private Message dispatch(Message msg) {
		List<MessageHandler> list = handlers.get(msg.getClass());
		if(list!=null && list.size()>0) {
			for(MessageHandler handler : list) {
				ThreadLocal message = (ThreadLocal)ReflectionUtils.getFieldVal(handler, "message", false);
				message.set(msg);
				Message handle = handler.handle(msg);
				if(handle != null) {
					handle.setFromUserName(msg.getToUserName());
					handle.setToUserName(msg.getFromUserName());
					handle.setCreateTime(SystemClock.now()/1000);
					handle.setMsgId(msg.getMsgId());
					return handle;
				}
			}
		}
		help.setFromUserName(msg.getToUserName());
		help.setToUserName(msg.getFromUserName());
		help.setCreateTime(SystemClock.now()/1000);
		help.setMsgId(msg.getMsgId());
		return help;
	}
	
	private Class getMessageType(Class clazz) {
		Class superClazz = clazz.getSuperclass();
		while(superClazz!=MessageHandler.class && superClazz!=EventHandler.class) {
			clazz = superClazz;
			superClazz = clazz.getSuperclass();
		}
		//class TextHandler extends MessageHandler<TextMessage>
		Type type = clazz.getGenericSuperclass();//MessageHandler<TextMessage>
		ParameterizedType ptype = (ParameterizedType)type;
		Type[] atype = ptype.getActualTypeArguments();//TextMessage
		return (Class)atype[0];
	}
	
	public static abstract class MessageHandler<M extends Message> {
		protected ThreadLocal<M> message = new ThreadLocal<>();
		public abstract Message handle(M msg);
		
		public static abstract class TextHandler extends MessageHandler<TextMessage> {
			@Override
			public Message handle(TextMessage message) {
				String content = message.getContent();
				if(StringUtil.isBlank(content)) return null;
				String handle = handle(content);
				if(StringUtil.isBlank(handle)) return null;
				message = new TextMessage();
				message.setContent(handle);
				return message;
			}

			public abstract String handle(String content);
			public boolean textTooLong(String text) {
				return text!=null && text.getBytes().length>weixinLimit();
			}
			public String textLimited(String text) {
				return StringUtil.limited(text, Charsets.UTF_8, weixinLimit());
			}
			public static int weixinLimit() {
				return NumberUtil.parseInt(RedisConfig.get("weixin.response.limit.length"), 450);
			}
		}
	}
	
	public static abstract class EventHandler<E extends Event> extends MessageHandler<E> {
		public E getEvent() { return message.get(); }
		
		public static abstract class ClickHandler extends EventHandler<ClickEvent> {
			public Message handle(ClickEvent event) {
				String key = event.getEventKey();
				if(StringUtil.isBlank(key)) return null;
				return handle(key);
			}
			
			public abstract Message handle(String key);
		}
	}
}
