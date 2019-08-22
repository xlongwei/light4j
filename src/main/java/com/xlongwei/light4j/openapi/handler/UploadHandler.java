package com.xlongwei.light4j.openapi.handler;

import java.io.File;

import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.FileUtil;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.IdWorker;
import com.xlongwei.light4j.util.StringUtil;
import com.xlongwei.light4j.util.UploadUtil;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData.FormValue;
import lombok.extern.slf4j.Slf4j;

/**
 * openapi upload handler
 * @author xlongwei
 *
 */
@Slf4j
public class UploadHandler extends AbstractHandler {

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		FormValue file = HandlerUtil.getFile(exchange, "file");
		if(file!=null && file.isFileItem()) {
			String fileName = file.getFileName();
			String path = "openapi/"+IdWorker.getId()+"."+FileUtil.getFileExt(fileName);
			File target = new File(UploadUtil.SAVE, path);
			boolean save = UploadUtil.save(file.getFileItem().getInputStream(), target);
			log.info("direct upload save={}, file={}, size={}K, path={}", save, fileName, target.length()/1024, path);
			if(save) {
				String url = UploadUtil.string(UploadUtil.URL + path);
				HandlerUtil.setResp(exchange, StringUtil.params("code", "0", "url", url));
			}
		}
	}

}
