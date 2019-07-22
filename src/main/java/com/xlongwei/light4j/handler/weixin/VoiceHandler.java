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
			TextMessage respMsg = new TextMessage();
			respMsg.setContent(recognition);
			AbstractMessage dispatch = null;
			if(recognition.length()>1) {
				TextMessage txtMsg = new TextMessage();
				//微信识别出的文字以句号结尾
				txtMsg.setContent(recognition.substring(0, recognition.length()-1));
				dispatch = WeixinUtil.dispatch(txtMsg);
			}
			return dispatch!=null ? dispatch : respMsg;
		}
		return null;
	}

}
