package com.xlongwei.light4j.handler.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;

import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.ConfigUtil;
import com.xlongwei.light4j.util.FileUtil;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.IdWorker;
import com.xlongwei.light4j.util.ImageUtil;
import com.xlongwei.light4j.util.NumberUtil;
import com.xlongwei.light4j.util.RedisConfig;
import com.xlongwei.light4j.util.StringUtil;
import com.xlongwei.light4j.util.UploadUtil;
import com.xlongwei.light4j.util.ZxingUtil;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.parameter.AddressType;
import ezvcard.parameter.EmailType;
import ezvcard.parameter.TelephoneType;
import ezvcard.property.Address;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData.FormValue;
import lombok.extern.slf4j.Slf4j;

/**
 * qrcode handler
 * @author xlongwei
 *
 */
@Slf4j
public class QrcodeHandler extends AbstractHandler {

	public void encode(HttpServerExchange exchange) throws Exception {
		String content = HandlerUtil.getParam(exchange, "content");
		Color forground = StringUtil.getColor(HandlerUtil.getParam(exchange, "forground"));
		Color background = StringUtil.getColor(HandlerUtil.getParam(exchange, "background"));
		int size = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "size"), 256);
		String logo = HandlerUtil.getParam(exchange, "logo");
		if(!StringUtil.isUrl(logo) && !ImageUtil.isBase64(logo)) {
			FormValue file = HandlerUtil.getFile(exchange, "img");
			if(file!=null && file.isFileItem()) {
				ByteArrayOutputStream stream = FileUtil.readStream(file.getFileItem().getInputStream());
				logo = ImageUtil.encode(stream.toByteArray(), null);
			}
		}
		byte[] encode = ZxingUtil.encode(content, size, logo, forground, background);
		if(encode!=null) {
			boolean base64Image = NumberUtil.parseBoolean(HandlerUtil.getParam(exchange, "base64Image"), true);
			if(base64Image) {
				HandlerUtil.setResp(exchange, StringUtil.params("base64Image", ImageUtil.encode(encode, null)));
			}else {
				String path = "image/"+IdWorker.getId()+".png";
				File target = new File(UploadUtil.SAVE_TEMP, path);
				FileUtil.writeBytes(target, encode);
				if(target.exists()) {
					HandlerUtil.setResp(exchange, StringUtil.params(UploadUtil.DOMAIN, UploadUtil.URL_TEMP, UploadUtil.PATH, path));
				}
			}
		}
	}
	
	public void decode(HttpServerExchange exchange) throws Exception {
		String url = HandlerUtil.getParam(exchange, "url");
		byte[] bytes = null;
		if(StringUtil.isUrl(url)) {
			bytes = FileUtil.bytes(url);
		}
		
		String base64 = HandlerUtil.getParam(exchange, "base64");
		if(bytes==null && ImageUtil.isBase64(base64)) {
			bytes = ImageUtil.decode(base64);
		}
		
		if(bytes==null) {
			FormValue file = HandlerUtil.getFile(exchange, "img");
			if(file!=null && file.isFileItem()) {
				bytes = FileUtil.readStream(file.getFileItem().getInputStream()).toByteArray();
			}
		}
		
		if(bytes!=null) {
			String decode = ZxingUtil.decode(bytes);
			if(StringUtil.hasLength(decode)) {
				HandlerUtil.setResp(exchange, StringUtil.params("content", decode));
			}
		}
	}
	
	public void barcode(HttpServerExchange exchange) throws Exception {
		String content = HandlerUtil.getParam(exchange, "content");
		int eighty = 80;
		if(StringUtil.isBlank(content) || content.length()>eighty) {
			return;
		}
		
		boolean addCheck = NumberUtil.parseBoolean(HandlerUtil.getParam(exchange, "addCheck"), false);
		if(StringUtil.isNumbers(content) && addCheck) {
			content += StringUtil.calcBarcode(content);
		}
		
		int margin = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "margin"), 5);
		int height = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "height"), 50);
		byte[] barcode = ZxingUtil.barcode(content, margin, height);
		
		if(barcode!=null) {
			boolean base64Image = NumberUtil.parseBoolean(HandlerUtil.getParam(exchange, "base64Image"), true);
			if(base64Image) {
				HandlerUtil.setResp(exchange, StringUtil.params("base64Image", ImageUtil.encode(barcode, null)));
			}else {
				String path = "image/"+IdWorker.getId()+".png";
				File target = new File(UploadUtil.SAVE_TEMP, path);
				FileUtil.writeBytes(target, barcode);
				if(target.exists()) {
					HandlerUtil.setResp(exchange, StringUtil.params(UploadUtil.DOMAIN, UploadUtil.URL_TEMP, UploadUtil.PATH, path));
				}
			}
		}
	}
	
	public void live(HttpServerExchange exchange) throws Exception {
		String code = StringUtil.firstNotBlank(HandlerUtil.getParam(exchange, "code"), "xlongwei.com");
		String url = HandlerUtil.getParam(exchange, "url");
		
		String userName = HandlerUtil.getParam(exchange, "showapi_userName");
		if(StringUtil.isBlank(userName)) {
			String liveqrcodeUserName = HandlerUtil.getParam(exchange, "liveqrcode_userName");
			if(!StringUtil.isBlank(liveqrcodeUserName)) {
				userName = RedisConfig.get("liveqrcode_userName."+liveqrcodeUserName);
			}
		}
		String clientNames = RedisConfig.get("liveqrcode.client.names");
		boolean isClient = !StringUtil.isBlank(userName) && StringUtil.splitContains(clientNames, userName);
		logger.info("userName: {}, isClient: {}, clientNames: {}", userName, isClient, clientNames);
		
		String codeKey = isClient ? "liveqrcode."+userName+"."+code : "livecode."+code;
		boolean isClientOrUrl = isClient || StringUtil.isUrl(url);
		if(!StringUtil.isBlank(url) && (isClientOrUrl)) {
			RedisConfig.set(codeKey, url);
		} else {
			url = RedisConfig.get(codeKey);
		}
		String liveUrl = ConfigUtil.FRONT_URL+"/open/qrcode/"+code+".html", userHost = null;
		if(isClient && StringUtil.hasLength(userHost=RedisConfig.get("liveqrcode."+userName))) {
			liveUrl = "http://"+userHost+"/"+code;
		}
		HandlerUtil.setResp(exchange, StringUtil.params("code", code, "url", url, "liveUrl", liveUrl));
	}
	
	public void vcard(HttpServerExchange exchange) throws Exception {
		String realName = HandlerUtil.getParam(exchange, "realName");
		String mobile = HandlerUtil.getParam(exchange, "mobile");
		if(StringUtil.isBlank(realName) || !StringUtil.isMobile(mobile)) {
			return;
		}
		VCard vcard = new VCard();
		vcard.setFormattedName(realName);
		vcard.addTelephoneNumber(mobile, TelephoneType.WORK);
		String email = HandlerUtil.getParam(exchange, "email");
		if(StringUtil.isEmail(email)) {
			vcard.addEmail(email, EmailType.WORK, EmailType.INTERNET);
		}
		String company = HandlerUtil.getParam(exchange, "company");
		if(!StringUtil.isBlank(company)) {
			vcard.setOrganization(company);
		}
		String title = HandlerUtil.getParam(exchange, "title");
		if(!StringUtil.isBlank(title)) {
			vcard.addTitle(title);
		}
		String website = HandlerUtil.getParam(exchange, "url");
		if(!StringUtil.isBlank(website)) {
			vcard.addUrl(website);
		}
		String address = HandlerUtil.getParam(exchange, "address");
		if(!StringUtil.isBlank(address)) {
			Address addr = new Address();
			addr.setStreetAddress(address);
			addr.getTypes().add(AddressType.WORK);
			vcard.addAddress(addr);
		}
		String vcardStr = Ezvcard.write(vcard).prodId(false).go();
		int size = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "size"), 256);
		String logo = HandlerUtil.getParam(exchange, "logo");
		if(!StringUtil.isUrl(logo) && !ImageUtil.isBase64(logo)) {
			FormValue file = HandlerUtil.getFile(exchange, "img");
			if(file!=null && file.isFileItem()) {
				ByteArrayOutputStream stream = FileUtil.readStream(file.getFileItem().getInputStream());
				logo = ImageUtil.encode(stream.toByteArray(), null);
			}
		}
		log.info("qrcode vcard size: {}, logo: {}, str: \n{}", size, logo, vcardStr);
		byte[] encode = ZxingUtil.encode(vcardStr, size, logo);
		if(encode!=null) {
			boolean base64Image = NumberUtil.parseBoolean(HandlerUtil.getParam(exchange, "base64Image"), true);
			if(base64Image) {
				HandlerUtil.setResp(exchange, StringUtil.params("base64Image", ImageUtil.encode(encode, null)));
			}else {
				String path = "image/"+IdWorker.getId()+".png";
				File target = new File(UploadUtil.SAVE_TEMP, path);
				FileUtil.writeBytes(target, encode);
				if(target.exists()) {
					HandlerUtil.setResp(exchange, StringUtil.params(UploadUtil.DOMAIN, UploadUtil.URL_TEMP, UploadUtil.PATH, path));
				}
			}
		}
	}

}
