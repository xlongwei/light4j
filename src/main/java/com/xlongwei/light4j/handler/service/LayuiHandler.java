package com.xlongwei.light4j.handler.service;

import java.io.OutputStream;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;
import org.jose4j.jwt.JwtClaims;

import com.networknt.security.JwtIssuer;
import com.networknt.utility.Constants;
import com.networknt.utility.HashUtil;
import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.ImageUtil;
import com.xlongwei.light4j.util.NumberUtil;
import com.xlongwei.light4j.util.RedisCache;
import com.xlongwei.light4j.util.ShiroUtil;
import com.xlongwei.light4j.util.StringUtil;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.ICaptcha;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import io.undertow.server.HttpServerExchange;
import lombok.extern.slf4j.Slf4j;

/**
 * layui handler
 * @author xlongwei
 *
 */
@Slf4j
public class LayuiHandler extends AbstractHandler {
	private static int width = 130, height = 38;
	private static String bearer = "Bearer ";
	
	public void captcha(HttpServerExchange exchange) throws Exception {
		String v = HandlerUtil.getParam(exchange, "v");
		ICaptcha captcha = CaptchaUtil.createShearCaptcha(width, height);
		captcha.createCode();
		String code = captcha.getCode();
		log.info("captcha code: {}, v: {}", code, v);
		RedisCache.set(ImageUtil.attr, v, code);
		OutputStream outputStream = exchange.getOutputStream();
		captcha.write(outputStream);
		outputStream.close();
	}
	
	public void login(HttpServerExchange exchange) throws Exception {
		boolean captchaOk = checkCaptcha(exchange);
		
		String username = HandlerUtil.getParam(exchange, "username");
		String password = HandlerUtil.getParam(exchange, "password");
		String checkPassword = ShiroUtil.getPassword(username);
		boolean passwordOk = checkPassword(password, checkPassword);
		
		HashMap<String, Object> map = MapUtil.newHashMap();
		map.put("code", captchaOk&&passwordOk ? 0 : 1);
		if(!captchaOk) {
			map.put("msg", "验证码错误"+RedisCache.get(ImageUtil.attr, HandlerUtil.getParam(exchange, "v")));
		}else if(!passwordOk) {
			if("admin".equals(username)) {
				map.put("msg", "密码错误"+checkPassword);
			}else {
				map.put("msg", "密码错误");
			}
		}
		
		if(captchaOk&&passwordOk) {
			token(username, map);
		}
        
        HandlerUtil.setResp(exchange, map);
	}
	
	private boolean checkCaptcha(HttpServerExchange exchange) {
		String vercode = HandlerUtil.getParam(exchange, "vercode");
		String v = HandlerUtil.getParam(exchange, "v");
		if(StringUtils.isNoneBlank(v, vercode)) {
			String check = RedisCache.get(ImageUtil.attr, v);
			boolean valid = vercode.equalsIgnoreCase(check);
			log.info("v:{}, expect:{}, vercode:{}, valid:{}", v, check, vercode, valid);
			if(valid) {
				return true;
			}
		}
		return false;
	}
	
	private boolean checkPassword(String password, String checkPassword) throws Exception {
		if(StringUtil.isBlank(password)) {
			return StringUtil.isBlank(checkPassword);
		}else if(password.equals(checkPassword)) {
			return true;
		}else if(!StringUtil.isBlank(checkPassword)) {
			int pos = checkPassword.indexOf(':');
			if(pos>0 && NumberUtil.parseInt(checkPassword.substring(0, pos), null)!=null) {
				return HashUtil.validatePassword(password.toCharArray(), checkPassword);
			}
		}
		return false;
	}

	private void token(String username, HashMap<String, Object> map) throws Exception {
		JwtClaims claims = JwtIssuer.getDefaultJwtClaims();
		claims.setClaim(Constants.USER_ID_STRING, username);
		String accessToken = JwtIssuer.getJwt(claims);
		//swagger认证时需要Bearer token
		String bearerToken = bearer + accessToken;
		map.put("data", StringUtil.params("access_token", bearerToken));
	}
	
	public void token(HttpServerExchange exchange) throws Exception {
		String username = HandlerUtil.getParam(exchange, "username");
		String password = HandlerUtil.getParam(exchange, "password");
		String checkPassword = ShiroUtil.getPassword(username);
		boolean passwordOk = checkPassword(password, checkPassword);
		HashMap<String, Object> map = MapUtil.newHashMap();
		map.put("code", passwordOk ? 0 : 1);
		if(passwordOk) {
			token(username, map);
		}else {
			map.put("msg", "密码错误");
		}
		HandlerUtil.setResp(exchange, map);
	}
	
	public void sms(HttpServerExchange exchange) throws Exception {
		HashMap<String, Object> map = MapUtil.newHashMap();
		boolean valid = checkCaptcha(exchange);
		map.put("code", valid ? 0 : 1);
		map.put("msg", valid ? StrUtil.EMPTY : "验证码错误");
		HandlerUtil.setResp(exchange, map);
	}
	
	public void forget(HttpServerExchange exchange) throws Exception {
		HashMap<String, Object> map = MapUtil.newHashMap();
		map.put("code", 0);
		HandlerUtil.setResp(exchange, map);
	}
	
	public void resetpass(HttpServerExchange exchange) throws Exception {
		HashMap<String, Object> map = MapUtil.newHashMap();
		map.put("code", 0);
		HandlerUtil.setResp(exchange, map);
	}
	
	public void reg(HttpServerExchange exchange) throws Exception {
		HashMap<String, Object> map = MapUtil.newHashMap();
		map.put("code", 0);
		HandlerUtil.setResp(exchange, map);
	}

}
