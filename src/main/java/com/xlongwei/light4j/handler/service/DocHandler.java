package com.xlongwei.light4j.handler.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.ArrayUtils;

import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.FileUtil;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.IdWorker;
import com.xlongwei.light4j.util.ImageUtil;
import com.xlongwei.light4j.util.JsonUtil;
import com.xlongwei.light4j.util.NumberUtil;
import com.xlongwei.light4j.util.PdfUtil;
import com.xlongwei.light4j.util.StringUtil;
import com.xlongwei.light4j.util.UploadUtil;
import com.xlongwei.light4j.util.WordUtil;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData.FormValue;

/**
 * doc handler
 * @author xlongwei
 *
 */
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
		if(doc.exists()==false) {
			return;
		}
		
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
		default:
			break;
		}
		if(toFile!=null && toFile.exists()) {
			Map<String, String> map = new HashMap<>(4);
			map.put(UploadUtil.DOMAIN, UploadUtil.URL_TEMP);
			map.put(UploadUtil.PATH, path);
			boolean base64File = NumberUtil.parseBoolean(HandlerUtil.getParam(exchange, "base64File"), false);
			if(base64File) {
				map.put("base64", Base64.encodeBase64String(FileUtil.readStream(toFile).toByteArray()));
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void toFill(HttpServerExchange exchange, File doc, File toFile) {
		Map<String, String> replaces = JsonUtil.parse(HandlerUtil.getParam(exchange, "replaces"), Map.class);
		List<List<Map<String, String>>> tables = JsonUtil.parse(HandlerUtil.getParam(exchange, "tables"), List.class);
		Map<String, String> pictures = JsonUtil.parse(HandlerUtil.getParam(exchange, "pictures"), Map.class);
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

	private File getDoc(HttpServerExchange exchange) throws IOException {
		File target = new File(UploadUtil.SAVE_TEMP, UploadUtil.path("word", IdWorker.getId()+".doc"));
		String url = HandlerUtil.getParam(exchange, "url");
		if(StringUtil.isUrl(url) && ArrayUtils.contains(exts, FileUtil.getFileExt(url))) {
			FileUtil.down(url, target);
		}
		if(!target.exists()) {
			FormValue doc = HandlerUtil.getFile(exchange, "doc");
			if(doc!=null && doc.isFileItem() && ArrayUtils.contains(exts, FileUtil.getFileExt(doc.getFileName()))) {
				UploadUtil.save(doc.getFileItem().getInputStream(), target);
			}
		}
		if(!target.exists()) {
			String base64 = HandlerUtil.getParam(exchange, "base64");
			base64 = ImageUtil.prefixRemove(base64);
			if(StringUtil.isBlank(base64)==false) {
				byte[] bs = Base64.decodeBase64(base64);
				UploadUtil.save(new ByteArrayInputStream(bs), target);
			}
		}
		return target;
	}

}
