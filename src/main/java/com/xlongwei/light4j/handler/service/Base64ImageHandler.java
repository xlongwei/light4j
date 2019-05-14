package com.xlongwei.light4j.handler.service;

import java.net.URL;

import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.FileUtil;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.ImageUtil;
import com.xlongwei.light4j.util.StringUtil;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData.FormValue;

/**
 * 转码图片为base64字符串
 * @author xlongwei
 *
 */
public class Base64ImageHandler extends AbstractHandler {

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		byte[] data = null; String ext = null;
		String url = HandlerUtil.getParam(exchange, "url");
		if(StringUtil.isUrl(url)) {
			try {
				data = FileUtil.readStream(new URL(url).openStream()).toByteArray();
				ext = FileUtil.getFileExt(url);
			}catch (Exception e) {}
		}else {
			FormValue file = HandlerUtil.getFile(exchange, "img");
			if(file!=null && file.isFileItem()) {
				try {
					data = FileUtil.readStream(file.getFileItem().getInputStream()).toByteArray();
					ext = FileUtil.getFileExt(file.getFileName());
				}catch (Exception e) {}
			}
		}
		if(data != null) {
			String base64Image = ImageUtil.encode(data, ext);
			HandlerUtil.setResp(exchange, StringUtil.params("image", base64Image));
		}
	}

}
