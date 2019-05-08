package com.xlongwei.light4j.util;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.lang3.ArrayUtils;

import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import com.qq.weixin.mp.aes.AesException;
import com.qq.weixin.mp.aes.WXBizMsgCrypt;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WeixinUtil {
	public static final String service = "https://api.weixin.qq.com/cgi-bin/", appid = "wx78b808148023e9fa";
	public static final String cache = "weixin", accessTokenKey = "access_token", jsapiTicketKey = "jsapi_ticket";
	private static final Map<Class<?>,Field[]> fieldsCache = new HashMap<>();
	private static final Map<String, Class<? extends Message>> classCache = new HashMap<>();
	private static final Map<String, WXBizMsgCrypt> wxBizMsgCrypts = new HashMap<>();
	
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
	
	public static WXBizMsgCrypt configAES(String appId, String token, String encodingAesKey) {
		try {
			WXBizMsgCrypt wxBizMsgCrypt = wxBizMsgCrypts.get(appId);
			if(wxBizMsgCrypt != null) return wxBizMsgCrypt;
			if(StringUtil.isBlank(token) || StringUtil.isBlank(encodingAesKey)) {
				token = RedisUtil.get(cache, "token."+appId);
				encodingAesKey = RedisUtil.get(cache, "aeskey."+appId);
				if(StringUtil.isBlank(token) || StringUtil.isBlank(encodingAesKey)) return null;
				wxBizMsgCrypts.put(appId, wxBizMsgCrypt = new WXBizMsgCrypt(token, encodingAesKey, appId));
			}else {
				wxBizMsgCrypts.put(appId, wxBizMsgCrypt = new WXBizMsgCrypt(token, encodingAesKey, appId));
				RedisUtil.persist(cache, "token."+appId, token);
				RedisUtil.persist(cache, "aeskey."+appId, encodingAesKey);
			}
			return wxBizMsgCrypt;
		}catch(Exception e) {
			log.warn(e.getMessage());
			return null;
		}
	}
	
	public static String encrypt(String appId, String replyMsg, String timestamp, String nonce) {
		try {
			WXBizMsgCrypt pc = configAES(appId, null, null);
			return pc==null ? null : pc.encryptMsg(replyMsg, timestamp, nonce);
		} catch (AesException e) {
			log.warn(e.getMessage());
		}
		return null;
	}
	
	public static String decrypt(String appId, String msgSignature, String timestamp, String nonce, String fromXML) {
		try {
			WXBizMsgCrypt pc = configAES(appId, null, null);
			return pc==null ? null : pc.decryptMsg(msgSignature, timestamp, nonce, fromXML);
		} catch (AesException e) {
			log.warn(e.getMessage());
		}
		return null;
	}
	
	static {
		try {
			String messagePattern = WeixinUtil.class.getName()+"\\$Message\\$\\w+Message";
			String eventPattern = WeixinUtil.class.getName()+"\\$Event\\$\\w+Event";
			for(ClassInfo classInfo : ClassPath.from(Message.class.getClassLoader()).getAllClasses()) {
				if(classInfo.getName().matches(messagePattern)) {
					Message message = (Message)classInfo.load().getConstructor().newInstance();
					classCache.put(message.getMsgType(), message.getClass());
				}else if(classInfo.getName().matches(eventPattern)) {
					Event event = (Event)classInfo.load().getConstructor().newInstance();
					classCache.put(event.getMsgType()+"."+event.getEvent(), event.getClass());
				}
			}
			log.info("size:{}",classCache.size());
			for(String type : classCache.keySet()) {
				log.info("type:{} => clazz:{}", type, classCache.get(type));
			}
		}catch(Exception e) {
			log.warn(e.getMessage());
		}
		
		Map<String, String> config = ConfigUtil.weixin();
		String token = config.get("token");
		String encodingAesKey = config.get("encodingAesKey");
		configAES(appid, token, encodingAesKey);
	}
	
	/** 微信公众号消息的封装 */
	public static abstract class Message {
	    private String ToUserName;  // 开发者微信号  
	    private String FromUserName;  // 发送方帐号（一个OpenID）  
	    private long CreateTime;  // 消息创建时间 （整型）  
	    protected String MsgType;  // 消息类型（text/image/location/link）  
	    private long MsgId;// 消息id，64位整型  
		public String getToUserName() { return ToUserName; }
		public void setToUserName(String toUserName) { ToUserName = toUserName; }
		public String getFromUserName() { return FromUserName; }
		public void setFromUserName(String fromUserName) { FromUserName = fromUserName; }
		public long getCreateTime() { return CreateTime; }
		public void setCreateTime(long createTime) { CreateTime = createTime; }
		public String getMsgType() { return MsgType; }
		public void setMsgType(String msgType) { MsgType = msgType; }
		public long getMsgId() { return MsgId; }
		public void setMsgId(long msgId) { MsgId = msgId; }
		
		public String toXML() {
			XMLObject xml = new XMLObject("xml");
			try {
				Field[] fields = Message.getFields(this.getClass());
				boolean isEvent = Type.event.equals(getMsgType());
				for(Field field : fields) {
					if(isEvent && "MsgId".equals(field.getName())) continue; //event没有MsgId字段
					Object value = field.get(this);
					String string = value==null ? "" : value.toString();
					if(!StringUtil.isBlank(string) && !string.matches("\\d+")) string = XMLObject.CDATA(string);
					xml.add(field.getName(), string);
				}
			}catch(Exception e) {
				log.info(e.getMessage());
			}
			return xml.toString();
		}
		public static Message fromXML(String xml) {
			XMLObject obj = XMLObject.fromXML(xml);
			String MsgType = obj.get("MsgType");
			Class<? extends Message> clazz = classCache.get(MsgType);
			boolean isEvent = false;
			if(clazz == null && Type.event.equals(MsgType)){
				isEvent = true;
				String event = obj.get("Event");
				if(Event.Type.subscribe.equals(event)) {
					String ticket = obj.get("Ticket");
					if(!StringUtil.isBlank(ticket)) {
						clazz = classCache.get(MsgType+"."+Event.Type.SCAN);
					}
				}
				if(clazz == null) clazz = classCache.get(MsgType+"."+event);
			}
			if(clazz != null) {
				try {
					Message msg = clazz.getConstructor().newInstance();
					Field[] fields = Message.getFields(msg.getClass());
					for(Field field : fields) {
						if(isEvent && "MsgId".equals(field.getName())) continue; //event没有MsgId字段
						String value = obj.get(field.getName());
						if(value != null) {
							Object convert = ConvertUtils.convert(value, field.getType());
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
				if(Event.class.isAssignableFrom(clazz)) fields = (Field[])ArrayUtils.addAll(fields, Message.class.getDeclaredFields());
				for(Field field : fields) field.setAccessible(true);
			}
			return fields;
		}
		
		public static class Type {
			public static final String text = "text";
			public static final String image = "image";
			public static final String voice = "voice";
			public static final String video = "video";
			public static final String music = "music";
			public static final String news = "news";
			public static final String shortvideo = "shortvideo";
			public static final String location = "location";
			public static final String link = "link";
			public static final String event = "event";
		}
	    
	    public static class TextMessage extends Message {
	    	private String Content; // 消息内容  
			public String getContent() { return Content; }
			public void setContent(String content) { Content = content; }
			public TextMessage() { this.MsgType = Type.text; }
	    }
	    public static class ImageMessage extends Message {
	    	private String PicUrl;//图片链接 
	    	private long MediaId;//图片消息媒体id，可以调用多媒体文件下载接口拉取数据。
			public String getPicUrl() { return PicUrl; }
			public void setPicUrl(String picUrl) { PicUrl = picUrl; }
			public long getMediaId() { return MediaId; }
			public void setMediaId(long mediaId) { MediaId = mediaId; }
			public ImageMessage() { this.MsgType = Type.image; }
	    }
	    public static class VoiceMessage extends Message {
	    	private long MediaId;//语音消息媒体id，可以调用多媒体文件下载接口拉取数据。
	    	private String Format;//语音格式，如amr，speex等 
	    	private String Recognition;//开通语音识别后有值
	    	public long getMediaId() { return MediaId; }
	    	public void setMediaId(long mediaId) { MediaId = mediaId; }
			public String getFormat() { return Format; }
			public void setFormat(String format) { Format = format; }
			public String getRecognition() { return Recognition; }
			public void setRecognition(String recognition) { Recognition = recognition; }
			public VoiceMessage() { this.MsgType = Type.voice; }
	    }
	    public static class VideoMessage extends Message {
	    	private long MediaId;//视频消息媒体id，可以调用多媒体文件下载接口拉取数据。 
	    	private long ThumbMediaId;//视频消息缩略图的媒体id，可以调用多媒体文件下载接口拉取数据。 
	    	public long getMediaId() { return MediaId; }
	    	public void setMediaId(long mediaId) { MediaId = mediaId; }
			public long getThumbMediaId() { return ThumbMediaId; }
			public void setThumbMediaId(long thumbMediaId) { ThumbMediaId = thumbMediaId; }
			public VideoMessage() { this.MsgType = Type.video; }
	    }
	    public static class ShortVideoMessage extends VideoMessage { 
	    	public ShortVideoMessage() { this.MsgType = Type.shortvideo; }
	    }
	    public static class LocationMessage extends Message {
	    	private double Location_X;//地理位置维度 
	    	private double Location_Y;//地理位置经度
	    	private int Scale;//地图缩放大小
	    	private String Label;//地理位置信息 
			public double getLocation_X() { return Location_X; }
			public void setLocation_X(double location_X) { Location_X = location_X; }
			public double getLocation_Y() { return Location_Y; }
			public void setLocation_Y(double location_Y) { Location_Y = location_Y; }
			public int getScale() { return Scale; }
			public void setScale(int scale) { Scale = scale; }
			public String getLabel() { return Label; }
			public void setLabel(String label) { Label = label; }
			public LocationMessage() { this.MsgType = Type.location; }
	    }
	    public static class LinkMessage extends Message {
	    	private String Title;//消息标题 
	    	private String Description;//消息描述
	    	private String Url;//消息链接
			public String getTitle() { return Title; }
			public void setTitle(String title) { Title = title; }
			public String getDescription() { return Description; }
			public void setDescription(String description) { Description = description; }
			public String getUrl() { return Url; }
			public void setUrl(String url) { Url = url; }
			public LinkMessage() { this.MsgType = Type.link; }
	    }
	}
	
    public static abstract class Event extends Message {
    	protected String Event;//事件类型，subscribe(订阅)、unsubscribe(取消订阅) 
		public String getEvent() { return Event; }
		public void setEvent(String event) { Event = event; }
		public Event() { this.MsgType = Message.Type.event; }
		
		public static class Type {
			public static final String subscribe = "subscribe";
			public static final String unsubscribe = "unsubscribe";
			public static final String SCAN = "SCAN";
			public static final String LOCATION = "LOCATION";
			public static final String CLICK = "CLICK";
			public static final String VIEW = "VIEW";
		}
		
		public static class SubscribeEvent extends Event {
			public SubscribeEvent() { this.Event = Type.subscribe; }
		}
		public static class UnsubscribeEvent extends Event {
			public UnsubscribeEvent() { this.Event = Type.unsubscribe; }
		}
		public static class ScanEvent extends Event {
			private String EventKey;//事件KEY值，qrscene_为前缀，后面为二维码的参数值 ，或创建二维码时的二维码scene_id
			private String Ticket;//二维码的ticket，可用来换取二维码图片
			public String getEventKey() { return EventKey; }
			public void setEventKey(String eventKey) { EventKey = eventKey; if(eventKey!=null && eventKey.startsWith("qrscene_")) { this.Event = Type.subscribe; } }
			public String getTicket() { return Ticket; }
			public void setTicket(String ticket) { Ticket = ticket; }
			public ScanEvent() { this.Event = Type.SCAN; }
		}
		public static class LocationEvent extends Event {
			private double Latitude;//地理位置纬度 
			private double Longitude;//地理位置经度 
			private double Precision;//地理位置精度 
			public double getLatitude() { return Latitude; }
			public void setLatitude(double latitude) { Latitude = latitude; }
			public double getLongitude() { return Longitude; }
			public void setLongitude(double longitude) { Longitude = longitude; }
			public double getPrecision() { return Precision; }
			public void setPrecision(double precision) { Precision = precision; }
			public LocationEvent() { this.Event = Type.LOCATION; }
		}
		public static class ClickEvent extends Event {
			private String EventKey;//事件KEY值，与自定义菜单接口中KEY值对应 
			public String getEventKey() { return EventKey; }
			public void setEventKey(String eventKey) { EventKey = eventKey; }
			public ClickEvent() { this.Event = Type.CLICK; }
		}
		public static class ViewEvent extends ClickEvent {
			private long MenuId;
			public long getMenuId() { return MenuId; }
			public void setMenuId(long menuId) { MenuId = menuId; }
			public ViewEvent() { this.Event = Type.VIEW; }
		}
    }
}
