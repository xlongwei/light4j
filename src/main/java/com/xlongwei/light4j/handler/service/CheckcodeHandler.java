package com.xlongwei.light4j.handler.service;

import java.awt.image.BufferedImage;

import org.apache.commons.lang3.RandomStringUtils;

import com.networknt.utility.Tuple;
import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.IdWorker;
import com.xlongwei.light4j.util.ImageUtil;
import com.xlongwei.light4j.util.MailUtil;
import com.xlongwei.light4j.util.NumberUtil;
import com.xlongwei.light4j.util.RedisCache;
import com.xlongwei.light4j.util.StringUtil;

import io.undertow.server.HttpServerExchange;
import lombok.extern.slf4j.Slf4j;

/**
 * checkcode handler
 * @author xlongwei
 *
 */
@Slf4j
public class CheckcodeHandler extends AbstractHandler {

	public void image(HttpServerExchange exchange) throws Exception {
		String checkcode = HandlerUtil.getParam(exchange, "checkcode");
		if(StringUtil.hasLength(checkcode)) {
			String image = ImageUtil.encode(ImageUtil.bytes(ImageUtil.create(checkcode)), null);
			HandlerUtil.setResp(exchange, StringUtil.params("image", image));
		}
	}
	
	public void audio(HttpServerExchange exchange) throws Exception {
		String checkcode = HandlerUtil.getParam(exchange, "checkcode");
		if(StringUtil.isBlank(checkcode)) {
			//0数字 1字母 2数字字母
			int type = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "type"), 0);
			int length = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "length"), 4);
			switch(type) {
			case 1: checkcode = RandomStringUtils.randomAlphabetic(length); break;
			case 2: checkcode = RandomStringUtils.randomAlphanumeric(length); break;
			default: checkcode = RandomStringUtils.randomNumeric(length); break;
			}
		}
		String voice = HandlerUtil.getParam(exchange, "voice");
		Tuple<String, byte[]> tuple = ImageUtil.speak(voice, checkcode);
		String audio = ImageUtil.encode(tuple.second, "wav");
		HandlerUtil.setResp(exchange, StringUtil.params("sid", tuple.first, "audio", audio));
	}
	
	public void code(HttpServerExchange exchange) throws Exception {
		int length = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "length"), 4);
		boolean specials = NumberUtil.parseBoolean(HandlerUtil.getParam(exchange, "specials"), false);
		int type = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "type"), -2);
		Tuple<String, BufferedImage> tuple = ImageUtil.create(length, specials, type);
		String image = ImageUtil.encode(ImageUtil.bytes(tuple.second), null);
		HandlerUtil.setResp(exchange, StringUtil.params("sid", tuple.first, "image", image));
	}
	
	public void check(HttpServerExchange exchange) throws Exception {
		String sid = HandlerUtil.getParam(exchange, "sid");
		String checkcode = HandlerUtil.getParam(exchange, "checkcode");
		if(StringUtil.hasLength(sid) && StringUtil.hasLength(checkcode)) {
			String check = RedisCache.get(ImageUtil.attr, sid);
			String valid = String.valueOf(checkcode.equalsIgnoreCase(check));
			log.info("sid:{}, expect:{}, checkcode:{}, valid:{}", sid, check, checkcode, valid);
			HandlerUtil.setResp(exchange, StringUtil.params("valid", valid));
		}
	}
	
	public void email(HttpServerExchange exchange) throws Exception {
		String toEmail = HandlerUtil.getParam(exchange, "toEmail");
		if(StringUtil.isEmail(toEmail)) {
			String title = StringUtil.firstNotBlank(HandlerUtil.getParam(exchange, "title"), "验证码");
			int minutes = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "minutes"), 10);
			String checkcode = HandlerUtil.getParam(exchange, "checkcode");
			if(StringUtil.isBlank(checkcode)) {
				checkcode = ImageUtil.random(NumberUtil.parseInt(HandlerUtil.getParam(exchange, "length"), 6), false);
			}
			String content = "您的验证码为："+checkcode+"，有效期"+minutes+"分钟，超时请重新获取；<br>如非本人操作，请忽略此邮件；";
			boolean send = MailUtil.send(toEmail, title, content);
			if(send) {
				String sid = String.valueOf(IdWorker.getId());
				RedisCache.set(ImageUtil.attr, sid, checkcode);
				log.info("email checkcode: {}, sid: {}", checkcode, sid);
				HandlerUtil.setResp(exchange, StringUtil.params("sid", sid, "code", checkcode));
			}
		}
	}

}
