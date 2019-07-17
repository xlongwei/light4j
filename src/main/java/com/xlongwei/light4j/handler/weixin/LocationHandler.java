package com.xlongwei.light4j.handler.weixin;

import com.xlongwei.light4j.util.WeixinUtil.AbstractMessage;
import com.xlongwei.light4j.util.WeixinUtil.AbstractMessage.LocationMessage;
import com.xlongwei.light4j.util.WeixinUtil.AbstractMessage.TextMessage;
import com.xlongwei.light4j.util.WeixinUtil.AbstractMessageHandler;

/**
 * location handler
 * @author xlongwei
 *
 */
public class LocationHandler extends AbstractMessageHandler<LocationMessage> {

	@Override
	public AbstractMessage handle(LocationMessage message) {
		double locationX = message.getLocationX();
		double locationY = message.getLocationY();
		int scale = message.getScale();
		String label = message.getLabel();
		TextMessage textMsg = new TextMessage();
		textMsg.setContent("位置："+label+"\n纬度："+locationX+"\n经度："+locationY+"\n放大比例："+scale);
		return textMsg;
	}

}
