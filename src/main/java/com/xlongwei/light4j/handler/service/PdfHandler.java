package com.xlongwei.light4j.handler.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;

import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.FileUtil;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.IdWorker;
import com.xlongwei.light4j.util.ImageUtil;
import com.xlongwei.light4j.util.NumberUtil;
import com.xlongwei.light4j.util.PdfUtil;
import com.xlongwei.light4j.util.SealUtil;
import com.xlongwei.light4j.util.StringUtil;
import com.xlongwei.light4j.util.UploadUtil;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData.FormValue;

/**
 * pdf handler
 * @author xlongwei
 *
 */
public class PdfHandler extends AbstractHandler {

	public void sealImage(HttpServerExchange exchange) throws Exception {
		byte[] seal = null;
		String person = HandlerUtil.getParam(exchange, "person");
		if(!StringUtil.isBlank(person)) {
			seal = SealUtil.seal(person);
		}else {
			String name = HandlerUtil.getParam(exchange, "name");
			String company = HandlerUtil.getParam(exchange, "company");
			String license = HandlerUtil.getParam(exchange, "license");
			if(StringUtil.isBlank(name) && StringUtil.isBlank(company) && StringUtil.isBlank(license)) {
				return;
			}else {
				seal = SealUtil.seal(name, company, license);
			}
		}
		
		if(seal != null) {
			HandlerUtil.setResp(exchange, StringUtil.params("image", ImageUtil.encode(seal, null)));
		}
	}
	
	public void seal(HttpServerExchange exchange) throws Exception {
		File pdf = getPdf(exchange);
		if(pdf==null || !pdf.exists()) {
			return;
		}
		
		String path = "pdf/"+IdWorker.getId()+".pdf";
		File target = new File(UploadUtil.SAVE_TEMP, path);
		int page = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "page"), 1);
		int x = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "page"), 360);
		int y = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "page"), 40);
		String person = HandlerUtil.getParam(exchange, "person");
		int height = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "page"), StringUtil.isBlank(person) ? 120 : 32);
		if(!StringUtil.isBlank(person)) {
			PdfUtil.seal(pdf, target, person, page, x, y, height);
		}else {
			String name = HandlerUtil.getParam(exchange, "name");
			String company = HandlerUtil.getParam(exchange, "company");
			String license = HandlerUtil.getParam(exchange, "license");
			if(StringUtil.isBlank(name) && StringUtil.isBlank(company) && StringUtil.isBlank(license)) {
				return;
			}else {
				PdfUtil.seal(pdf, target, name, company, license, page, x, y, height);
			}
		}
		
		if(target.exists()) {
			Map<String, String> map = new HashMap<>(4);
			map.put(UploadUtil.DOMAIN, UploadUtil.URL_TEMP);
			map.put(UploadUtil.PATH, path);
			boolean base64File = NumberUtil.parseBoolean(HandlerUtil.getParam(exchange, "base64File"), false);
			if(base64File) {
				map.put("base64", Base64.encodeBase64String(FileUtil.readStream(target).toByteArray()));
			}
		}
	}

	private File getPdf(HttpServerExchange exchange) throws Exception {
		File target = new File(UploadUtil.SAVE_TEMP, "pdf/"+IdWorker.getId()+".pdf");
		String url = HandlerUtil.getParam(exchange, "url");
		if(StringUtil.isUrl(url)) {
			FileUtil.down(url, target);
		}else {
			String base64 = HandlerUtil.getParam(exchange, "base64");
			base64 = StringUtil.isBlank(base64) ? null : ImageUtil.prefixRemove(base64);
			if(!StringUtil.isBlank(base64)) {
				byte[] bs = Base64.decodeBase64(base64);
				UploadUtil.save(new ByteArrayInputStream(bs), target);
			}else {
				FormValue pdf = HandlerUtil.getFile(exchange, "pdf");
				if(pdf!=null && pdf.isFileItem()) {
					UploadUtil.save(pdf.getFileItem().getInputStream(), target);
				}
			}
		}
		return target.exists() ? target : null;
	}
}
