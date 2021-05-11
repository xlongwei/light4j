package com.xlongwei.light4j;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.shiro.util.AntPathMatcher;
import org.apache.shiro.util.PatternMatcher;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.config.Config;
import com.networknt.config.RefreshScope;
import com.networknt.server.Server;
import com.networknt.server.ServerConfig;
import com.xlongwei.light4j.util.RedisConfig;
import com.xlongwei.light4j.util.ShiroUtil;

import cn.hutool.core.util.RandomUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * redis test
 * @author xlongwei
 */
@Slf4j
public class RedisTest {

	/**
	 * jwt shiro default infos
	 */
	@Test public void shiro() {
		RedisConfig.delete(RedisConfig.CACHE, "shiro.urls");
		//roles["admin,user","client"] 校验角色：(admin and user) or client
		//perms[openapi:upload:*,"service:upload:upload,temp"] 校验权限：openapi:upload:* or service:upload:upload,temp
		//pattern=/openapi/upload*/** 可以匹配多种路径
		RedisConfig.hset(RedisConfig.CACHE, ShiroUtil.URLS, "/openapi/upload*/**", "roles[\"admin,user\",admin],perms[openapi:upload:*,\"service:upload:upload,delete\"]");
		Map<String, String> hgetAll = RedisConfig.hgetAll(RedisConfig.CACHE, "shiro.urls");
		log.info("shiro.urls={}", hgetAll);
		
		//[users]
		//username=password,role1,role2
		RedisConfig.hset(RedisConfig.CACHE, ShiroUtil.USERS, "admin", "123456,admin");
		//JwtMockHandler授权的token：user_id=steve
		RedisConfig.hset(RedisConfig.CACHE, ShiroUtil.USERS, "steve", "123456,admin");
		log.info("shiro.users={}", RedisConfig.hgetAll(RedisConfig.CACHE, ShiroUtil.USERS));
		//[roles]
		//role1=permission1,permission2
		RedisConfig.hset(RedisConfig.CACHE, ShiroUtil.ROLES, "admin", "*:*:*");
		log.info("shiro.roles={}", RedisConfig.hgetAll(RedisConfig.CACHE, ShiroUtil.ROLES));
	}
	
	@Test public void pathMatcher() {
		//pattern=/openapi/upload*/** 可以匹配多种路径
		PatternMatcher pathMatcher = new AntPathMatcher();
		String[] patterns = {"/openapi/upload*","/openapi/upload/*","/openapi/upload","/openapi/upload*/**","/openapi/upload/**","/**/upload*/**"};
		String[] paths = {"/openapi/upload","/openapi/upload/","/openapi/upload.json","/openapi/upload/temp","/openapi/upload/temp/","/openapi/upload/temp.json"};
		for(String pattern : patterns) {
			log.info("pattern={}", pattern);
			for(String path : paths) {
				log.info("path={}, matches={}", path, pathMatcher.matches(pattern, path));
			}
			log.info("");
		}
	}
	
	@Test public void sequence() {
		String name = RandomUtil.randomString(10);
		RedisConfig.Sequence.update(name, -1);
		Assert.assertEquals(1, RedisConfig.Sequence.next(name));
		RedisConfig.Sequence.update(name, 0);
		Assert.assertEquals(1, RedisConfig.Sequence.next(name));
		RedisConfig.Sequence.update(name, 10);
		Assert.assertEquals(11, RedisConfig.Sequence.next(name));
		RedisConfig.Sequence.update(name, -1);
		Assert.assertEquals(1, RedisConfig.Sequence.next(name));
		RedisConfig.Sequence.update(name, -1);
	}
	
	@Test public void config() throws Exception {
		String homeDir = System.getProperty("user.home");
		System.setProperty(Config.LIGHT_4J_CONFIG_DIR, homeDir);
		
		File test = new File(homeDir + "/server.yml");
		int oldPort = 8080, newPort = 8081;
		
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> map = new HashMap<>();
		map.put("httpPort", oldPort);
		mapper.writeValue(test, map);
		
		ServerConfig serverConfig1 = (ServerConfig)Config.getInstance().getJsonObjectConfig(Server.SERVER_CONFIG_NAME, ServerConfig.class);
		ServerConfig serverConfig2 = RefreshScope.getJsonObjectConfig(Server.SERVER_CONFIG_NAME, ServerConfig.class);
		assertEquals(serverConfig1.getHttpPort(), serverConfig2.getHttpPort());
		
		map.put("httpPort", newPort);
		mapper.writeValue(test, map);
		Config.getInstance().clear();
		
		assertEquals(serverConfig1.getHttpPort(), oldPort);
		assertEquals(serverConfig2.getHttpPort(), newPort);
		
		test.delete();
	}
}
