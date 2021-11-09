package com.xlongwei.light4j.handler.service;

import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.Map;

import com.networknt.utility.Tuple;
import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.IdWorker;
import com.xlongwei.light4j.util.ImageUtil;
import com.xlongwei.light4j.util.MailUtil;
import com.xlongwei.light4j.util.NumberUtil;
import com.xlongwei.light4j.util.RedisCache;
import com.xlongwei.light4j.util.RedisConfig;
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
		}else {
			String sid = HandlerUtil.getParam(exchange, "sid");
			if(sid!=null && (sid=sid.trim()).length()>13) {
				int length = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "length"), 4);
				boolean specials = NumberUtil.parseBoolean(HandlerUtil.getParam(exchange, "specials"), false);
				int type = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "type"), -2);
				String[] special = type<-1 ? null : ImageUtil.special(type);
				String code = special!=null&& special.length>0 ? special[0] : null;
				String check = null;
				if(code==null) {
					check = code = ImageUtil.random(length, specials);
				}else {
					check = special.length>1 ? special[1] : code;
				}
				log.info("captcha code: {}, check: {}, sid: {}", code, check, sid);
				RedisCache.set(ImageUtil.attr, sid, check);
				OutputStream outputStream = exchange.getOutputStream();
				outputStream.write(ImageUtil.bytes(ImageUtil.create(code)));
				outputStream.close();
			}
		}
	}
	
	public void code(HttpServerExchange exchange) throws Exception {
		int length = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "length"), 4);
		boolean specials = NumberUtil.parseBoolean(HandlerUtil.getParam(exchange, "specials"), false);
		int type = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "type"), -2);
		Tuple<String, String> codecheck = ImageUtil.create(length, specials, type);
		Tuple<String, BufferedImage> tuple = ImageUtil.create(codecheck.first, codecheck.second);
		String image = ImageUtil.encode(ImageUtil.bytes(tuple.second), null);
		Map<String, String> params = StringUtil.params("sid", tuple.first, "image", image);
		if(NumberUtil.parseBoolean(HandlerUtil.getParam(exchange, "secure"), true)==false) {
			params.put("code", codecheck.first);
			if(!codecheck.first.equals(codecheck.second) && !StringUtil.isBlank(codecheck.second)) {
				params.put("check", codecheck.second);
			}
		}
		HandlerUtil.setResp(exchange, params);
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
			String showapiUserName = HandlerUtil.getShowapiUserName(exchange);
			String template = "您的验证码为{0}，有效期{1}分钟，超时请重新获取。";
			if(!StringUtil.isBlank(showapiUserName)) {
				//Your verification code is {0}, valid for {1} minutes, please get it again after timeout.
				template = StringUtil.firstNotBlank(RedisConfig.get("email."+showapiUserName), template);
			}
			String content = MessageFormat.format(template, checkcode, minutes);
			boolean send = NumberUtil.parseBoolean(RedisConfig.get("email.switch"), true) ? MailUtil.send(toEmail, title, content) : true;
			if(send) {
				String sid = String.valueOf(IdWorker.getId());
				RedisCache.set(ImageUtil.attr, sid, checkcode);
				log.info("email checkcode: {}, sid: {}", checkcode, sid);
				HandlerUtil.setResp(exchange, StringUtil.params("sid", sid, "code", checkcode));
			}
		}
	}

}
