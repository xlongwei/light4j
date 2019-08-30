package com.xlongwei.light4j;

import java.util.Map;

import org.apache.shiro.util.AntPathMatcher;
import org.apache.shiro.util.PatternMatcher;
import org.junit.Test;

import com.xlongwei.light4j.util.RedisConfig;
import com.xlongwei.light4j.util.ShiroUtil;

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
}
