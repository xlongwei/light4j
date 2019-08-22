package com.xlongwei.light4j.handler.service;

import java.util.HashMap;
import java.util.Map;

import org.jose4j.jwt.JwtClaims;

import com.networknt.security.JwtIssuer;
import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.ImageUtil;
import com.xlongwei.light4j.util.RedisCache;
import com.xlongwei.light4j.util.StringUtil;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.ICaptcha;
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
	private static ICaptcha captcha = null;
	private static int width = 130, height = 38;
	private static String bearer = "Bearer ";
	
	public void captcha(HttpServerExchange exchange) throws Exception {
		captcha = CaptchaUtil.createShearCaptcha(width, height);
		captcha.createCode();
		String v = HandlerUtil.getParam(exchange, "v");
		log.info("captcha code: {}, v: {}", captcha.getCode(), v);
		RedisCache.set(ImageUtil.attr, v, captcha.getCode());
		captcha.write(exchange.getOutputStream());
	}
	
	public void login(HttpServerExchange exchange) throws Exception {
		String vercode = HandlerUtil.getParam(exchange, "vercode");
		String v = HandlerUtil.getParam(exchange, "v");
		String check = RedisCache.get(ImageUtil.attr, v);
		String valid = String.valueOf(vercode.equalsIgnoreCase(check));
		log.info("v:{}, expect:{}, vercode:{}, valid:{}", v, check, vercode, valid);
		
		Map<String, Object> resMap = new HashMap<>(4);
		resMap.put("code", 0);
		resMap.put("msg", StrUtil.EMPTY);
		
		JwtClaims claims = JwtIssuer.getDefaultJwtClaims();
		claims.setClaim("user_id", "openapi");
		String accessToken = JwtIssuer.getJwt(claims);
		//swagger认证时需要Bearer token
		String bearerToken = bearer + accessToken;
		resMap.put("data", StringUtil.params("access_token", bearerToken));
        
        HandlerUtil.setResp(exchange, resMap);
	}

}
