package com.xlongwei.light4j.handler.service;

import java.io.ByteArrayInputStream;
import java.io.File;

import org.apache.commons.codec.binary.Base64;

import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.IdWorker;
import com.xlongwei.light4j.util.ImageUtil;
import com.xlongwei.light4j.util.StringUtil;
import com.xlongwei.light4j.util.UploadUtil;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData.FormValue;

/**
 * 上传文件和确认路径
 * @author xlongwei
 *
 */
public class UploadHandler extends AbstractHandler {

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		String type = StringUtil.firstNotBlank(HandlerUtil.getParam(exchange, "type"), "image");
		if(UploadUtil.CONFIRM.equals(type)) {
			String name = HandlerUtil.getParam(exchange, "name");
			boolean move = UploadUtil.move(name, UploadUtil.CONFIRM);
			HandlerUtil.setResp(exchange, StringUtil.params(UploadUtil.CONFIRM, move ? "1" : "0"));
		}else {
			String base64 = HandlerUtil.getParam(exchange, "base64");
			String path = type;
			File target = null;
			if(ImageUtil.isBase64(base64)) {
				String ext = ImageUtil.prefixFormat(base64);
				path = type + "/" + IdWorker.getId() + "." + ext;
				byte[] bs = Base64.decodeBase64(ImageUtil.prefixRemove(base64));
				target = new File(UploadUtil.SAVE_TEMP, path);
				UploadUtil.save(new ByteArrayInputStream(bs), target);
			}else {
				FormValue file = HandlerUtil.getFile(exchange, "file");
				if(file!=null && file.isFileItem()) {
					String fileName = file.getFileName();
					path = UploadUtil.path(type, fileName);
					target = new File(UploadUtil.SAVE_TEMP, path);
					UploadUtil.save(file.getFileItem().getInputStream(), target);
				}
			}
			if(target!=null && target.exists()) {
				HandlerUtil.setResp(exchange, StringUtil.params("domain", UploadUtil.URL_TEMP, "path", path));
			}
		}
	}

}
