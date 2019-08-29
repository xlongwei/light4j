package com.xlongwei.light4j;

import java.util.Map;

import org.junit.Test;

import com.xlongwei.light4j.util.RedisConfig;

public class RedisTest {

	@Test public void shiro() {
		//roles["admin,user","client"] 校验角色：(admin and user) or client
		//perms[openapi:upload:*,"service:upload:upload,temp"] 校验权限：openapi:upload:* or service:upload:upload,temp
		RedisConfig.hset(RedisConfig.CACHE, "shiro.urls", "/openapi/upload/*", "roles[\"admin,user\",admin],perms[openapi:upload:*,\"service:upload:upload,delete\"]");
		Map<String, String> hgetAll = RedisConfig.hgetAll(RedisConfig.CACHE, "shiro.urls");
		System.out.println(hgetAll);
		//RedisConfig.delete(RedisConfig.CACHE, "shiro.urls");
	}
}
