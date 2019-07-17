package com.xlongwei.light4j.handler.weixin;

import com.xlongwei.light4j.util.FileUtil;
import com.xlongwei.light4j.util.StringUtil;
import com.xlongwei.light4j.util.WeixinUtil.AbstractMessage;
import com.xlongwei.light4j.util.WeixinUtil.AbstractMessage.ImageMessage;
import com.xlongwei.light4j.util.WeixinUtil.AbstractMessage.TextMessage;
import com.xlongwei.light4j.util.WeixinUtil.AbstractMessageHandler;
import com.xlongwei.light4j.util.ZxingUtil;

/**
 * image handler
 * @author xlongwei
 *
 */
public class ImageHandler extends AbstractMessageHandler<ImageMessage> {

	@Override
	public AbstractMessage handle(ImageMessage message) {
		String url = message.getPicUrl();
		if(StringUtil.isUrl(url)) {
			byte[] bytes = FileUtil.bytes(url);
			if(bytes != null) {
				String decode = ZxingUtil.decode(bytes);
				if(!StringUtil.isBlank(decode)) {
					TextMessage txtMsg = new TextMessage();
					txtMsg.setContent(decode);
					return txtMsg;
				}
			}
		}
		return null;
	}

}
