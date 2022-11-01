package com.xlongwei.light4j.handler.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSON;
import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.FileUtil;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.HttpUtil;
import com.xlongwei.light4j.util.IdWorker;
import com.xlongwei.light4j.util.ImageUtil;
import com.xlongwei.light4j.util.JsonUtil;
import com.xlongwei.light4j.util.NumberUtil;
import com.xlongwei.light4j.util.PdfUtil;
import com.xlongwei.light4j.util.StringUtil;
import com.xlongwei.light4j.util.TaskUtil;
import com.xlongwei.light4j.util.UploadUtil;
import com.xlongwei.light4j.util.WordUtil;
import com.xlongwei.light4j.util.FileUtil.CharsetNames;
import com.xlongwei.light4j.util.FileUtil.TextWriter;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData.FormValue;
import lombok.extern.slf4j.Slf4j;

/**
 * doc handler
 * @author xlongwei
 *
 */
@Slf4j
public class DocHandler extends AbstractHandler {
	private String[] exts = {"doc","docx","xls","xlsx","txt","html","ppt","pptx",
			"rtf","csv","tsv","svg","wiki","xhtml","odt","ods","odp","odg","sxw","sxc","sxi","wpd"};
	private String[] imageFileExts = {"png", "jpg", "jpeg", "gif"};
	
	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		String path = exchange.getAttachment(AbstractHandler.PATH);
		if(StringUtil.isBlank(path)) {
			return;
		}
		
		File doc = getDoc(exchange);
		log.info("doc file: {}, exists: {}", doc.getAbsolutePath(), doc.exists());
		if(doc.exists()==false) {
			return;
		}
		
		String callback = HandlerUtil.getParam(exchange, "callback");
		if(StringUtil.isUrl(callback)) {
			final String callbackId = String.valueOf(IdWorker.getId());
			TaskUtil.submit(()->{
				Map<String, String> map = handle(exchange, path, doc);
				if(map.isEmpty()) {
					map.put("error", "convert failed");
				}else {
					map.put("url", map.get(UploadUtil.DOMAIN)+map.get(UploadUtil.PATH));
				}
				map.put("callback", callbackId);
				HttpUtil.post(callback, map);
			});
			HandlerUtil.setResp(exchange, Collections.singletonMap("callback", callbackId));
		}else {
			handle(exchange, path, doc);
		}
	}

	private Map<String, String> handle(HttpServerExchange exchange, String path, File doc) {
		File toFile = null;
		switch(path) {
		case "toPdf":
		case "toHtml":
		case "convert":
			path = "toPdf".equals(path) ? "pdf" : ("toHtml".equals(path) ? "html" : (StringUtil.isBlank(path=HandlerUtil.getParam(exchange, "output")) || !ArrayUtils.contains(exts, path) ? "pdf" : path));
			toFile = new File(UploadUtil.SAVE_TEMP, path = UploadUtil.path(path, IdWorker.getId() + "." + path));
			PdfUtil.doc2pdf(doc, toFile);
			break;
		case "toFill":
			toFile = new File(UploadUtil.SAVE_TEMP, path = UploadUtil.path("fill", IdWorker.getId() + "." + FileUtil.getFileExt(doc)));
			toFill(exchange, doc, toFile);
			break;
		case "base64":
			toFile = doc;
			path = "word/" + doc.getName();
			break;
		default:
			break;
		}
		log.info("toFile: {}, exists: {}", toFile==null?"null":toFile.getAbsolutePath(), toFile!=null&&toFile.exists());
		Map<String, String> map = new HashMap<>(4);
		if(toFile!=null && toFile.exists()) {
			map.put(UploadUtil.DOMAIN, UploadUtil.URL_TEMP);
			map.put(UploadUtil.PATH, path);
			boolean base64File = "base64".equals(exchange.getAttachment(AbstractHandler.PATH))
					? StringUtil.isBlank(HandlerUtil.getParam(exchange, "base64")) && !NumberUtil.parseBoolean(HandlerUtil.getParam(exchange, "append"), false)
					: NumberUtil.parseBoolean(HandlerUtil.getParam(exchange, "base64File"), false);
			if(base64File) {
				map.put("base64", Base64.encodeBase64URLSafeString(FileUtil.readStream(toFile).toByteArray()));
			}
			HandlerUtil.setResp(exchange, map);
		}
		return map;
	}

	@SuppressWarnings("unchecked")
	private void toFill(HttpServerExchange exchange, File doc, File toFile) {
		Map<String, String> replaces = JsonUtil.parse(getParam(exchange, "replaces"), Map.class);
		List<List<Map<String, String>>> tables = JsonUtil.parse(getParam(exchange, "tables"), List.class);
		Map<String, String> pictures = JsonUtil.parse(getParam(exchange, "pictures"), Map.class);
		if(pictures!=null && pictures.size()>0) {
			if(replaces == null) {
				replaces = new HashMap<>(2);
			}
			for(String key : pictures.keySet()) {
				String value = pictures.get(key);
				if(StringUtil.isBlank(value)) {
					continue;
				}
				if(StringUtil.isUrl(value)) {
					String imgFileExt = FileUtil.getFileExt(value).toLowerCase();
					if(ArrayUtils.contains(imageFileExts, imgFileExt)) {
						value = ImageUtil.encode(FileUtil.bytes(value), imgFileExt);
					}
				}
				if(ImageUtil.isBase64(value)) {
					replaces.put(key, value);
				}
			}
		}
		WordUtil.doc2fill(doc, toFile, replaces, tables);
	}

	private String getParam(HttpServerExchange exchange, String name) {
		Object obj = HandlerUtil.getObject(exchange, name, Object.class);
		if(obj !=null ) {//支持json参数{"replaces":{"var":"value"}}
			return obj instanceof String ? (String) obj : JSON.toJSONString(obj);
		}
		return null;
	}

	private File getDoc(HttpServerExchange exchange) throws IOException {
		File target = new File(UploadUtil.SAVE_TEMP, UploadUtil.path("word", IdWorker.getId()+".docx"));
		String url = HandlerUtil.getParam(exchange, "url");
		boolean append = NumberUtil.parseBoolean(HandlerUtil.getParam(exchange, "append"), false);
		String fileName = HandlerUtil.getParam(exchange, "fileName");
		if(StringUtil.isUrl(url)) {
			if(url.startsWith(UploadUtil.URL)) {
				target = new File(UploadUtil.SAVE, url.substring(UploadUtil.URL.length()));
				if(append && StringUtils.isNotBlank(fileName)) {
					File decode = new File(target.getParent(), fileName);
					String base64 = FileUtil.readString(target, CharsetNames.UTF_8);
					byte[] bs = Base64.decodeBase64(base64);
					FileUtil.writeBytes(decode, bs);
					target = decode;
				}
			}else {
				String ext = FileUtil.getFileExt(StringUtils.defaultIfBlank(HandlerUtil.getParam(exchange, "fileName"), url));
				if(ArrayUtils.contains(exts, ext) && !FileUtil.getFileExt(target).equalsIgnoreCase(ext)) {
					target = new File(target.getParent(), FileUtil.getFileName(target) + "." + ext);
				}
			}
			if(!target.exists()) FileUtil.down(url, target);
		}
		if(!target.exists()) {
			FormValue doc = HandlerUtil.getFile(exchange, "doc");
			if(doc!=null && doc.isFileItem() && ArrayUtils.contains(exts, FileUtil.getFileExt(doc.getFileName()))) {
				String ext = FileUtil.getFileExt(doc.getFileName());
				if(ArrayUtils.contains(exts, ext) && !FileUtil.getFileExt(target).equalsIgnoreCase(ext)) {
					target = new File(target.getParent(), FileUtil.getFileName(target) + "." + ext);
				}
				UploadUtil.save(doc.getFileItem().getInputStream(), target);
				if(append && StringUtils.isNotBlank(fileName)){
					File decode = new File(target.getParent(), fileName);
					TextWriter writer = new TextWriter(decode, CharsetNames.UTF_8, true);
					writer.write(FileUtil.readString(target, CharsetNames.UTF_8));
					writer.close();
					target = decode;
				}
			}
		}
		if(!target.exists()) {
			String base64 = HandlerUtil.getParamOrBody(exchange, "base64");
			base64 = StringUtil.isBlank(base64) ? null : ImageUtil.prefixRemove(base64);
			if(!StringUtil.isBlank(base64)) {
				byte[] bs = Base64.decodeBase64(base64);
				String ext = StringUtil.isBlank(fileName) ? null : FileUtil.getFileExt(fileName);
				if(!StringUtil.isBlank(ext) && !FileUtil.getFileExt(target).equalsIgnoreCase(ext)) {
					target = new File(target.getParent(), FileUtil.getFileName(target) + "." + ext);
				}
				if(append && StringUtils.isNotBlank(fileName)){
					if (target.getParentFile().exists() == false) {
						target.getParentFile().mkdirs();
					}
					TextWriter writer = new TextWriter(target, CharsetNames.UTF_8, true);
					writer.writeln(base64);
					writer.close();
				}else{
					UploadUtil.save(new ByteArrayInputStream(bs), target);
				}
			}
		}
		return target;
	}

}
