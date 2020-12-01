package com.xlongwei.light4j.handler;

import java.io.File;
import java.util.Deque;
import java.util.Map;

import com.networknt.handler.LightHttpHandler;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.NumberUtil;
import com.xlongwei.light4j.util.StringUtil;
import com.xlongwei.light4j.util.UploadUtil;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData.FormValue;
import lombok.extern.slf4j.Slf4j;

/**
 * 上传服务
 * @author xlongwei
 *
 */
@Slf4j
public class UploadHandler implements LightHttpHandler {

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();
		String service = queryParameters.remove("*").getFirst();
		log.info("{} {}", exchange.getRequestMethod(), exchange.getRequestURI());
		int dot = service.indexOf('.');
		String name = dot==-1 ? service : service.substring(0, dot);
		HandlerUtil.parseBody(exchange);
		if(name.length()==0 || UploadUtil.TEMP.equals(name) || UploadUtil.DIRECT.equals(name)) {
			//上传文件
			upload(exchange, name);
		}else if(UploadUtil.CONFIRM.equals(name) || UploadUtil.TRASH.equals(name)) {
			//移动文件
			move(exchange, name);
		}
		HandlerUtil.sendResp(exchange);
	}

	private void upload(HttpServerExchange exchange, String name) {
		FormValue file = HandlerUtil.getFile(exchange, "file");
		if(file!=null && file.isFileItem()) {
			String type = StringUtil.firstNotBlank(HandlerUtil.getParam(exchange, "type"), "image");
			//temp方式重命名 direct方式默认不重命名，保存目录也不同
			boolean temp = UploadUtil.DIRECT.equals(name) ? false : true;
			boolean rename = temp ? true : NumberUtil.parseBoolean(HandlerUtil.getParam(exchange, "rename"), Boolean.FALSE);
			String fileName = file.getFileName();
			String path = rename ? UploadUtil.path(type, fileName) : type+"/"+fileName;
			File target = new File(temp ? UploadUtil.SAVE_TEMP : UploadUtil.SAVE, path);
			try {
				boolean save = UploadUtil.save(file.getFileItem().getInputStream(), target);
				log.info("direct upload save={}, file={}, size={}K, path={}", save, fileName, target.length()/1024, path);
				if(save) {
					String string = UploadUtil.string((temp ? UploadUtil.URL_TEMP : UploadUtil.URL) + "," + path);
					exchange.putAttachment(HandlerUtil.RESP, string);
				}
			}catch(Exception e) {
				log.warn("fail to {} upload file, ex:{}", name, e.getMessage());
			}
		}
	}
	
	private void move(HttpServerExchange exchange, String name) {
		String path = HandlerUtil.getParam(exchange, "path");
		boolean move = UploadUtil.move(path, name);
		exchange.putAttachment(HandlerUtil.RESP, move ? "1" : "0");
	}
}
