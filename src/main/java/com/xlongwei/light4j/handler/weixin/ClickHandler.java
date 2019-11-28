package com.xlongwei.light4j.handler.weixin;

import com.xlongwei.light4j.util.StringUtil;
import com.xlongwei.light4j.util.WeixinUtil;
import com.xlongwei.light4j.util.WeixinUtil.AbstractEventHandler.AbstractClickHandler;
import com.xlongwei.light4j.util.WeixinUtil.AbstractMessage;
import com.xlongwei.light4j.util.WeixinUtil.AbstractMessage.TextMessage;

/**
 * 将click事件的key作为消息转发处理
 * @author xlongwei
 *
 */
public class ClickHandler extends AbstractClickHandler {

	@Override
	public AbstractMessage handle(String key) {
		if(StringUtil.isBlank(key)) {
			return null;
		}else {
			TextMessage textMsg = new TextMessage();
			textMsg.setContent(key);
			return WeixinUtil.dispatch(textMsg);
		}
	}

}
