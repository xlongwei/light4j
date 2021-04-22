package com.xlongwei.light4j.util;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSONObject;
import com.qq.weixin.mp.aes.AesException;
import com.qq.weixin.mp.aes.WXBizMsgCrypt;
import com.xlongwei.light4j.util.FileUtil.Charsets;
import com.xlongwei.light4j.util.IdWorker.SystemClock;
import com.xlongwei.light4j.util.WeixinUtil.AbstractEvent.ClickEvent;
import com.xlongwei.light4j.util.WeixinUtil.AbstractMessage.TextMessage;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ReflectUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * weixin util
 * @author xlongwei
 *
 */
@Slf4j
@SuppressWarnings({"rawtypes","unchecked"})
public class WeixinUtil {
	public static final String service = "https://api.weixin.qq.com/cgi-bin/", appid = StringUtil.firstNotBlank(System.getProperty("weixin.appid"), "wx78b808148023e9fa");
	public static final String appidTest = StringUtil.firstNotBlank(System.getProperty("weixin.appidTest"), "wx5bb3e90365f54b7a"), touserTest = StringUtil.firstNotBlank(System.getProperty("weixin.touserTest"), "gh_f6216a9ae70b");
	public static final String cache = "weixin", accessTokenKey = "access_token", jsapiTicketKey = "jsapi_ticket";
	private static final Map<Class<?>,Field[]> fieldsCache = new HashMap<>(32);
	private static final Map<String, Class<? extends AbstractMessage>> classCache = new HashMap<>(16);
	private static final Map<String, WXBizMsgCrypt> wxBizMsgCrypts = new HashMap<>(4);
	private static final Map<Class, List<AbstractMessageHandler>> handlers = new HashMap<>(16);
	
	/** 获取access_token */
	public static String accessToken(String appid, String appsecret) {
		String key = accessTokenKey+"."+appid;
		String accessToken = RedisConfig.get(cache, key);
		if(StringUtil.isBlank(accessToken)) {
			String token = HttpUtil.get(service+"token", StringUtil.params("grant_type","client_credential","appid", appid, "secret", appsecret));
			accessToken = JsonUtil.get(token, "access_token");
			if(StringUtil.hasLength(accessToken)) {
				int expiresIn = JsonUtil.getInt(token, "expires_in");
				if(expiresIn > 0) {
					expiresIn = (expiresIn - 10);
					RedisConfig.set(cache, key, accessToken, expiresIn);
				}
			}
		}
		return accessToken;
	}
	
	/** 计算签名signature */
	public static String signature(String ticket, String url, String noncestr, String timestamp) {
		if(!StringUtil.isBlank(noncestr) && StringUtil.isUrl(url)) {
			StringBuilder string1 = new StringBuilder();
			string1.append("jsapi_ticket="+ticket);
			string1.append("&noncestr="+noncestr);
			string1.append("&timestamp="+timestamp);
			string1.append("&url="+url);
			String signature = StringUtil.digest(string1.toString(), "SHA");
			log.info(string1.toString());
			log.info("signature: "+signature);
			return signature;
		}
		return null;
	}
	
	public static boolean checkSignature(String signature, String timestamp, String nonce, String token) {
		String[] arr = new String[] { token, timestamp, nonce };  
        // 将token、timestamp、nonce三个参数进行字典序排序  
        Arrays.sort(arr);  
        StringBuilder content = new StringBuilder();  
        for (int i = 0; i < arr.length; i++) {  
            content.append(arr[i]);  
        }
        String sign = StringUtil.digest(content.toString(), "SHA-1");
        return signature.equalsIgnoreCase(sign);
	}
	
	/** 配置加密解密类 */
	public static WXBizMsgCrypt configAES(String appId, String token, String encodingAesKey) {
		try {
			WXBizMsgCrypt wxBizMsgCrypt = wxBizMsgCrypts.get(appId);
			if(wxBizMsgCrypt != null) {
				return wxBizMsgCrypt;
			}
			if(StringUtil.isBlank(token) || StringUtil.isBlank(encodingAesKey)) {
				token = RedisConfig.get(cache, "token."+appId);
				encodingAesKey = RedisConfig.get(cache, "aeskey."+appId);
				if(StringUtil.isBlank(token) || StringUtil.isBlank(encodingAesKey)) {
					return null;
				}
				wxBizMsgCrypts.put(appId, wxBizMsgCrypt = new WXBizMsgCrypt(token, encodingAesKey, appId));
			}else {
				wxBizMsgCrypts.put(appId, wxBizMsgCrypt = new WXBizMsgCrypt(token, encodingAesKey, appId));
				RedisConfig.persist(cache, "token."+appId, token);
				RedisConfig.persist(cache, "aeskey."+appId, encodingAesKey);
			}
			return wxBizMsgCrypt;
		}catch(Exception e) {
			log.warn(e.getMessage());
			return null;
		}
	}
	
	/** 加密响应消息 */
	public static String encrypt(String appId, String replyMsg, String timestamp, String nonce) {
		try {
			WXBizMsgCrypt pc = configAES(appId, null, null);
			return pc==null ? null : pc.encryptMsg(replyMsg, timestamp, nonce);
		} catch (AesException e) {
			log.warn(e.getMessage());
		}
		return null;
	}
	
	/** 解密请求消息 */
	public static String decrypt(String appId, String msgSignature, String timestamp, String nonce, String fromXML) {
		try {
			WXBizMsgCrypt pc = configAES(appId, null, null);
			return pc==null ? null : pc.decryptMsg(msgSignature, timestamp, nonce, fromXML);
		} catch (AesException e) {
			log.warn(e.getMessage());
		}
		return null;
	}
	
	/** 发送模板消息 */
	public static JSONObject templateSend(String accessToken, JSONObject body) {
		return JsonUtil.parse(cn.hutool.http.HttpUtil.post(service+"message/template/send?access_token="+accessToken, body.toJSONString()));
	}
	
	/** 注册消息处理类 */
	public static void register(AbstractMessageHandler handler) {
		Class type = getMessageType(handler.getClass());
		List<AbstractMessageHandler> msgHandlers = handlers.get(type);
		if(msgHandlers==null) {
			handlers.put(type, msgHandlers = new ArrayList<>());
		}
		msgHandlers.add(handler);
		log.info("register {} => {}", type.getSimpleName(), handler.getClass().getName());
	}
	
	/** 方法处理消息，默认响应帮助信息 */
	public static AbstractMessage dispatch(AbstractMessage msg) {
		List<AbstractMessageHandler> list = handlers.get(msg.getClass());
		if(list!=null && list.size()>0) {
			for(AbstractMessageHandler handler : list) {
				ThreadLocal message = (ThreadLocal)ReflectUtil.getFieldValue(handler, "message");
				message.set(msg);
				try {
					AbstractMessage handle = handler.handle(msg);
					if(handle != null) {
						handle.setFromUserName(msg.getToUserName());
						handle.setToUserName(msg.getFromUserName());
						handle.setCreateTime(SystemClock.now()/1000);
						handle.setMsgId(msg.getMsgId());
						return handle;
					}
				}finally {
					message.remove();
				}
			}
		}
		return null;
	}
	
	public static Class getMessageType(Class clazz) {
		Class superClazz = clazz.getSuperclass();
		while(superClazz!=AbstractMessageHandler.class && superClazz!=AbstractEventHandler.class) {
			clazz = superClazz;
			superClazz = clazz.getSuperclass();
		}
		//class TextHandler extends MessageHandler<TextMessage>
		Type type = clazz.getGenericSuperclass();
		//MessageHandler<TextMessage>
		ParameterizedType ptype = (ParameterizedType)type;
		Type[] atype = ptype.getActualTypeArguments();
		//TextMessage
		return (Class)atype[0];
	}
	
	static {
		classCache.putAll(AbstractMessage.getMessages());
		classCache.putAll(AbstractEvent.getEvents());
		log.info("weixin message and event size: {}", classCache.size());
		for(String type : classCache.keySet()) {
			log.info("type:{} => clazz:{}", type, classCache.get(type));
		}
		String token = RedisConfig.get("weixin.token");
		String encodingAesKey = RedisConfig.get("weixin.encodingAesKey");
		configAES(appid, token, encodingAesKey);
	}
	
	/** 微信公众号消息的封装 */
	@Getter
	@Setter
	public static abstract class AbstractMessage {
		/** 开发者微信号  */
	    private String toUserName;
	    /**  发送方帐号（一个OpenID）   */
	    private String fromUserName;
	    /**  消息创建时间 （整型）   */
	    private long createTime;
	    /**  消息类型（text/image/location/link）   */
	    protected String msgType;
	    /**  消息id，64位整型   */
	    private long msgId;
		
		public String toXML() {
			XmlObject xml = new XmlObject("xml");
			try {
				Field[] fields = AbstractMessage.getFields(this.getClass());
				boolean isEvent = Type.event.equals(getMsgType());
				for(Field field : fields) {
					String name = StringUtils.capitalize(field.getName());
					if(isEvent && "MsgId".equals(name))
					 {
						//event没有MsgId字段
						continue; 
					}
					Object value = field.get(this);
					String string = value==null ? "" : value.toString();
					if(!StringUtil.isBlank(string) && !string.matches("\\d+")) {
						string = XmlObject.cdata(string);
					}
					xml.add(name, string);
				}
			}catch(Exception e) {
				log.info(e.getMessage());
			}
			return xml.toString();
		}
		public static AbstractMessage fromXML(String xml) {
			XmlObject obj = XmlObject.fromXML(xml);
			if(obj == null) {
				return null;
			}
			String msgType = obj.get("MsgType");
			if(StringUtil.isBlank(msgType)) {
				return null;
			}else {
				msgType = msgType.trim();
			}
			Class<? extends AbstractMessage> clazz = classCache.get(msgType);
			boolean isEvent = false;
			if(clazz == null && Type.event.equals(msgType)){
				isEvent = true;
				String event = obj.get("Event");
				if(AbstractEvent.Type.subscribe.equals(event)) {
					String ticket = obj.get("Ticket");
					if(!StringUtil.isBlank(ticket)) {
						clazz = classCache.get(msgType+"."+AbstractEvent.Type.SCAN);
					}
				}
				if(clazz == null) {
					clazz = classCache.get(msgType+"."+event);
				}
			}
			if(clazz != null) {
				try {
					AbstractMessage msg = clazz.getConstructor().newInstance();
					Field[] fields = AbstractMessage.getFields(msg.getClass());
					for(Field field : fields) {
						if(isEvent && "MsgId".equalsIgnoreCase(field.getName()))
						 {
							//event没有MsgId字段
							continue; 
						}
						String value = obj.get(field.getName());
						if(value != null) {
							Object convert = Convert.convert(field.getType(), value);
							field.set(msg, convert);
						}
					}
					return msg;
				} catch (Exception e) {
					log.info(e.getMessage());
				}
			}
			return null;
		}
		private static Field[] getFields(Class<?> clazz) {
			Field[] fields = fieldsCache.get(clazz);
			if(fields == null) {
				fields = (Field[])ArrayUtils.addAll(clazz.getDeclaredFields(), clazz.getSuperclass().getDeclaredFields());
				if(AbstractEvent.class.isAssignableFrom(clazz)) {
					fields = (Field[])ArrayUtils.addAll(fields, AbstractMessage.class.getDeclaredFields());
				}
				for(Field field : fields) {
					field.setAccessible(true);
				}
			}
			return fields;
		}
		public static class Type {
			public static final String text = "text";
			public static final String image = "image";
			public static final String voice = "voice";
			public static final String video = "video";
			public static final String shortvideo = "shortvideo";
			public static final String location = "location";
			public static final String link = "link";
			public static final String event = "event";
		}
		public static Map<String, Class<? extends AbstractMessage>> getMessages(){
			Map<String, Class<? extends AbstractMessage>> map = new HashMap<>(8);
			map.put(Type.image, ImageMessage.class);
			map.put(Type.link, LinkMessage.class);
			map.put(Type.location, LocationMessage.class);
			map.put(Type.shortvideo, ShortVideoMessage.class);
			map.put(Type.text, TextMessage.class);
			map.put(Type.video, VideoMessage.class);
			map.put(Type.voice, VoiceMessage.class);
			return map;
		}
	    @Getter
	    @Setter
	    public static class TextMessage extends AbstractMessage {
	    	/**  消息内容   */
	    	private String content;
			public TextMessage() { this.msgType = Type.text; }
	    }
	    @Getter
	    @Setter
	    public static class ImageMessage extends AbstractMessage {
	    	/** 图片链接  */
	    	private String picUrl;
	    	/** 图片消息媒体id，可以调用多媒体文件下载接口拉取数据。 */
	    	private long mediaId;
			public ImageMessage() { this.msgType = Type.image; }
	    }
	    @Getter
	    @Setter
	    public static class VoiceMessage extends AbstractMessage {
	    	/** 语音消息媒体id，可以调用多媒体文件下载接口拉取数据。 */
	    	private long mediaId;
	    	/** 语音格式，如amr，speex等  */
	    	private String format;
	    	/** 开通语音识别后有值 */
	    	private String recognition;
			public VoiceMessage() { this.msgType = Type.voice; }
	    }
	    @Getter
	    @Setter
	    public static class VideoMessage extends AbstractMessage {
	    	/** 视频消息媒体id，可以调用多媒体文件下载接口拉取数据。  */
	    	private long mediaId;
	    	/** 视频消息缩略图的媒体id，可以调用多媒体文件下载接口拉取数据。  */
	    	private long thumbMediaId;
			public VideoMessage() { this.msgType = Type.video; }
	    }
	    public static class ShortVideoMessage extends VideoMessage { 
	    	public ShortVideoMessage() { this.msgType = Type.shortvideo; }
	    }
	    @Getter
	    @Setter
	    public static class LocationMessage extends AbstractMessage {
	    	/** 地理位置维度  */
	    	private double locationX;
	    	/** 地理位置经度 */
	    	private double locationY;
	    	/** 地图缩放大小 */
	    	private int scale;
	    	/** 地理位置信息  */
	    	private String label;
			public LocationMessage() { this.msgType = Type.location; }
	    }
	    @Getter
	    @Setter
	    public static class LinkMessage extends AbstractMessage {
	    	/** 消息标题  */
	    	private String title;
	    	/** 消息描述 */
	    	private String description;
	    	/** 消息链接 */
	    	private String url;
			public LinkMessage() { this.msgType = Type.link; }
	    }
	}
	@Getter
	@Setter
    public static abstract class AbstractEvent extends AbstractMessage {
		/** 事件类型，subscribe(订阅)、unsubscribe(取消订阅)  */
    	protected String event;
		public AbstractEvent() { this.msgType = AbstractMessage.Type.event; }
		
		public static class Type {
			public static final String subscribe = "subscribe";
			public static final String unsubscribe = "unsubscribe";
			public static final String SCAN = "SCAN";
			public static final String LOCATION = "LOCATION";
			public static final String CLICK = "CLICK";
			public static final String VIEW = "VIEW";
		}
		public static Map<String, Class<? extends AbstractEvent>> getEvents() {
			Map<String, Class<? extends AbstractEvent>> map = new HashMap<>(6);
			map.put(AbstractMessage.Type.event+"."+Type.subscribe, SubscribeEvent.class);
			map.put(AbstractMessage.Type.event+"."+Type.unsubscribe, UnsubscribeEvent.class);
			map.put(AbstractMessage.Type.event+"."+Type.SCAN, ScanEvent.class);
			map.put(AbstractMessage.Type.event+"."+Type.LOCATION, LocationEvent.class);
			map.put(AbstractMessage.Type.event+"."+Type.CLICK, ClickEvent.class);
			map.put(AbstractMessage.Type.event+"."+Type.VIEW, ViewEvent.class);
			return map;
		}
		public static class SubscribeEvent extends AbstractEvent {
			public SubscribeEvent() { this.event = Type.subscribe; }
		}
		public static class UnsubscribeEvent extends AbstractEvent {
			public UnsubscribeEvent() { this.event = Type.unsubscribe; }
		}
		@Getter
		@Setter
		public static class ScanEvent extends AbstractEvent {
			/** 事件KEY值，qrscene_为前缀，后面为二维码的参数值 ，或创建二维码时的二维码scene_id */
			private String eventKey;
			/** 二维码的ticket，可用来换取二维码图片 */
			private String ticket;
			public ScanEvent() { this.event = Type.SCAN; }
		}
		@Getter
		@Setter
		public static class LocationEvent extends AbstractEvent {
			/** 地理位置纬度  */
			private double latitude;
			/** 地理位置经度  */
			private double longitude;
			/** 地理位置精度  */
			private double precision;
			public LocationEvent() { this.event = Type.LOCATION; }
		}
		@Getter
		@Setter
		public static class ClickEvent extends AbstractEvent {
			/** 事件KEY值，与自定义菜单接口中KEY值对应  */
			private String eventKey;
			public ClickEvent() { this.event = Type.CLICK; }
		}
		@Getter
		@Setter
		public static class ViewEvent extends ClickEvent {
			private long menuId;
			public ViewEvent() { this.event = Type.VIEW; }
		}
    }
    
	public static abstract class AbstractMessageHandler<M extends AbstractMessage> {
		protected ThreadLocal<M> message = new ThreadLocal<>();
		/**
		 * 处理消息，返回响应
		 * @param msg
		 * @return
		 */
		public abstract AbstractMessage handle(M msg);
		
		public static abstract class AbstractTextHandler extends AbstractMessageHandler<TextMessage> {
			@Override
			public AbstractMessage handle(TextMessage message) {
				String content = message.getContent();
				if(StringUtil.isBlank(content)) {
					return null;
				}
				String handle = handle(content);
				if(StringUtil.isBlank(handle)) {
					return null;
				}
				message = new TextMessage();
				message.setContent(handle);
				return message;
			}
			/**
			 * 处理文本消息，返回文本
			 * @param content
			 * @return
			 */
			public abstract String handle(String content);
			public boolean textTooLong(String text) {
				return text!=null && text.getBytes(Charsets.UTF_8).length>weixinLimit();
			}
			public String textLimited(String text) {
				return StringUtil.limited(text, Charsets.UTF_8, weixinLimit());
			}
			public static int weixinLimit() {
				return NumberUtil.parseInt(RedisConfig.get("weixin.response.limit.length"), 450);
			}
			protected static String split = "[ ,;:　，；：、]+";
		}
	}
	
	public static abstract class AbstractEventHandler<E extends AbstractEvent> extends AbstractMessageHandler<E> {
		public E getEvent() { return message.get(); }
		
		public static abstract class AbstractClickHandler extends AbstractEventHandler<ClickEvent> {
			@Override
			public AbstractMessage handle(ClickEvent event) {
				String key = event.getEventKey();
				if(StringUtil.isBlank(key)) {
					return null;
				}
				return handle(key);
			}
			/**
			 * 处理点击事件
			 * @param key
			 * @return
			 */
			public abstract AbstractMessage handle(String key);
		}
	}    
}
