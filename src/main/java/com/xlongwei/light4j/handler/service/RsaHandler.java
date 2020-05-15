package com.xlongwei.light4j.handler.service;

import java.security.KeyPair;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.networknt.utility.StringUtils;
import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.IdWorker;
import com.xlongwei.light4j.util.RedisConfig;
import com.xlongwei.light4j.util.RsaUtil;

import cn.hutool.core.map.MapUtil;
import io.undertow.server.HttpServerExchange;

/**
 * rsa handler
 * @author xlongwei
 *
 */
public class RsaHandler extends AbstractHandler {

	private static final String PUBLIC_KEY = "publicKey";
	private static final String PRIVATE_KEY = "privateKey";
	private static final String PUBLIC_CACHE_KEY = "publicCacheKey";
	private static final String PRIVATE_CACHE_KEY = "privateCacheKey";
	private static final String RSA_PUBKEY = "rsa.pubkey.";
	private static final String RSA_PRIKEY = "rsa.prikey.";

	public void keypair(HttpServerExchange exchange) throws Exception {
		KeyPair keyPair = RsaUtil.getKeyPair();
		String publicKey = RsaUtil.getKeyString(keyPair.getPublic());
		String privateKey = RsaUtil.getKeyString(keyPair.getPrivate());
		setKeys(exchange, publicKey, privateKey);
	}

	public void config(HttpServerExchange exchange) throws Exception {
		String publicKey = HandlerUtil.getParam(exchange, PUBLIC_KEY);
		String privateKey = HandlerUtil.getParam(exchange, PRIVATE_KEY);
		if(StringUtils.isNotBlank(publicKey) && StringUtils.isNotBlank(privateKey) && RsaUtil.verify(publicKey, privateKey)) {
			setKeys(exchange, publicKey, privateKey);
		}
	}

	private void setKeys(HttpServerExchange exchange, String publicKey, String privateKey) {
		Map<String, String> map = MapUtil.newHashMap(4);
		map.put(PUBLIC_KEY, publicKey);
		map.put(PRIVATE_KEY, privateKey);
		map.put(PUBLIC_CACHE_KEY, RSA_PUBKEY+IdWorker.getId());
		map.put(PRIVATE_CACHE_KEY, RSA_PRIKEY+IdWorker.getId());
		RedisConfig.set(map.get(PUBLIC_CACHE_KEY), publicKey);
		RedisConfig.set(map.get(PRIVATE_CACHE_KEY), privateKey);
		int expire = (int)TimeUnit.DAYS.toSeconds(30);
		RedisConfig.expire(RedisConfig.CACHE, map.get(PUBLIC_CACHE_KEY), expire);
		RedisConfig.expire(RedisConfig.CACHE, map.get(PRIVATE_CACHE_KEY), expire);
		HandlerUtil.setResp(exchange, map);
	}
	
	public void encrypt(HttpServerExchange exchange) throws Exception {
		String publicKey = HandlerUtil.getParam(exchange, PUBLIC_KEY, PUBLIC_CACHE_KEY);
		String data = HandlerUtil.getParam(exchange, "data");
		if(StringUtils.isNotBlank(publicKey) && StringUtils.isNotBlank(data)) {
			data = RsaUtil.encrypt(RsaUtil.getPublicKey(publicKey), data);
			HandlerUtil.setResp(exchange, MapUtil.of("data", data));
		}
	}
	
	public void decrypt(HttpServerExchange exchange) throws Exception {
		String privateKey = HandlerUtil.getParam(exchange, PRIVATE_KEY, PRIVATE_CACHE_KEY);
		String data = HandlerUtil.getParam(exchange, "data");
		if(StringUtils.isNotBlank(privateKey) && StringUtils.isNotBlank(data)) {
			data = RsaUtil.decrypt(RsaUtil.getPrivateKey(privateKey), data);
			HandlerUtil.setResp(exchange, MapUtil.of("data", data));
		}
	}
	
	public void sign(HttpServerExchange exchange) throws Exception {
		String privateKey = HandlerUtil.getParam(exchange, PRIVATE_KEY, PRIVATE_CACHE_KEY);
		String data = HandlerUtil.getParam(exchange, "data");
		if(StringUtils.isNotBlank(privateKey) && StringUtils.isNotBlank(data)) {
			data = RsaUtil.sign(RsaUtil.getPrivateKey(privateKey), data);
			HandlerUtil.setResp(exchange, MapUtil.of("sign", data));
		}
	}
	
	public void verify(HttpServerExchange exchange) throws Exception {
		String publicKey = HandlerUtil.getParam(exchange, PUBLIC_KEY, PUBLIC_CACHE_KEY);
		String data = HandlerUtil.getParam(exchange, "data");
		String sign = HandlerUtil.getParam(exchange, "sign");
		if(StringUtils.isNotBlank(publicKey) && StringUtils.isNotBlank(data)) {
			boolean verify = RsaUtil.verify(RsaUtil.getPublicKey(publicKey), data, sign);
			HandlerUtil.setResp(exchange, MapUtil.of("verify", String.valueOf(verify)));
		}
	}

}
