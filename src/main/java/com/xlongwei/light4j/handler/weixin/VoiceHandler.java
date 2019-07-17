package com.xlongwei.light4j.handler.weixin;

import com.xlongwei.light4j.util.StringUtil;
import com.xlongwei.light4j.util.WeixinUtil;
import com.xlongwei.light4j.util.WeixinUtil.AbstractMessage;
import com.xlongwei.light4j.util.WeixinUtil.AbstractMessage.TextMessage;
import com.xlongwei.light4j.util.WeixinUtil.AbstractMessage.VoiceMessage;
import com.xlongwei.light4j.util.WeixinUtil.AbstractMessageHandler;

/**
 * voice handler
 * @author xlongwei
 *
 */
public class VoiceHandler extends AbstractMessageHandler<VoiceMessage> {

	@Override
	public AbstractMessage handle(VoiceMessage msg) {
		String recognition = msg.getRecognition();
		if(!StringUtil.isBlank(recognition)) {
			TextMessage txtMsg = new TextMessage();
			txtMsg.setContent(recognition);
			AbstractMessage dispatch = WeixinUtil.dispatch(txtMsg);
			return dispatch!=null ? dispatch : txtMsg;
		}
		return null;
	}

}
