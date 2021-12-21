package com.xlongwei.light4j.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// import org.apache.shiro.util.StringUtils;

import com.xlongwei.light4j.util.FileUtil.CharsetNames;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * shiro util
 * @author xlongwei
 *
 */
@Slf4j
public class ShiroUtil {
	/**{admin=123456,admin}*/
	public static final String USERS = "shiro.users";
	/**{admin=*:*:*}*/
	public static final String ROLES = "shiro.roles";
	/**
	 * /openapi/upload/*=roles["admin,user",admin],perms[openapi:upload:*,"service:upload:upload,delete"]
	 */
	public static final String URLS = "shiro.urls";
	public static final String MESSAGE = "ShiroUtil.reload";
	private static final Map<String, String> users = new ConcurrentHashMap<>();
	private static final Map<String, String> roles = new ConcurrentHashMap<>();
	private static final Map<String, String> urls = new ConcurrentHashMap<>();
	
	/** 获取用户密码 */
	public static String getPassword(String username) {
		if(StrUtil.isNotBlank(username)) {
//			String passwordRoles = RedisConfig.hget(RedisConfig.CACHE, USERS, username);
			String passwordRoles = users.get(username);
			List<String> passwordRolesList = StrUtil.splitTrim(passwordRoles, ',');
			if(CollUtil.isNotEmpty(passwordRolesList)) {
				return passwordRolesList.get(0);
			}
		}
		return StrUtil.EMPTY;
	}
	
	/** 获取角色列表 */
	public static Set<String> getRoles(String username) {
		if(StrUtil.isNotBlank(username)) {
//			String passwordRoles = RedisConfig.hget(RedisConfig.CACHE, USERS, username);
			String passwordRoles = users.get(username);
			List<String> passwordRolesList = StrUtil.splitTrim(passwordRoles, ',');
			if(CollUtil.isNotEmpty(passwordRolesList)) {
				passwordRolesList.remove(0);
			}
			return new HashSet<>(passwordRolesList);
		}
		return Collections.emptySet();
	}
	
	/** 获取权限列表 */
	public static Set<String> getPerms(Set<String> roles) {
		if(CollUtil.isNotEmpty(roles)) {
			Set<String> permList = new HashSet<>();
			roles.forEach(role -> {
//				String perms = RedisConfig.hget(RedisConfig.CACHE, ROLES, role);
				String perms = ShiroUtil.roles.get(role);
				if(!StringUtil.isBlank(perms)) {
					// String[] permArray = StringUtils.split(perms, StringUtils.DEFAULT_DELIMITER_CHAR, '"', '"', true, true);
					// for(String perm : permArray) {
					// 	if(perm.indexOf('"') > -1) {
					// 		perm = perm.substring(0, perm.length()-1);
					// 	}
					// 	permList.add(perm);
					// }
				}
			});
			return permList;
		}
		return Collections.emptySet();
	}
	
	/** 获取url列表 */
	public static Map<String, String> getUrls() {
//		Map<String, String> urls = RedisConfig.hgetAll(RedisConfig.CACHE, "shiro.urls");
		return urls;
	}
	
	/** 重新加载配置 */
	public static void reload(boolean file) {
		log.info("reload file={}", file);
		if(file) {
			List<String> lines = FileUtil.readLines(ConfigUtil.stream("shiro.ini"), CharsetNames.UTF_8);
			Map<String, String> map = new HashMap<>();
			for(String line : lines) {
				if(StringUtil.isBlank(line) || line.startsWith("#")) {
					continue;
				}else {
					if(line.contains("[users]")) {
						//依次读取配置行，然后替换缓存map，最后更新到redis
					}else if(line.contains("[roles]")) {
						RedisConfig.delete(RedisConfig.CACHE, USERS);
						RedisConfig.hmset(RedisConfig.CACHE, USERS, map);
						map.clear();
					}else if(line.contains("[urls]")) {
						RedisConfig.delete(RedisConfig.CACHE, ROLES);
						RedisConfig.hmset(RedisConfig.CACHE, ROLES, map);
						map.clear();
					}else {
						String[] split = line.split("[=]", 2);
						map.put(split[0], StringUtil.trim(split[1]));
					}
				}
			}
			RedisConfig.delete(RedisConfig.CACHE, URLS);
			RedisConfig.hmset(RedisConfig.CACHE, URLS, map);
			RedisPubsub.pub(MESSAGE);
		}else {
			//主app从文件读取配置并更新到redis，所有app定时通过redis更新配置
			try{
				users.putAll(RedisConfig.hgetAll(RedisConfig.CACHE, USERS));
				roles.putAll(RedisConfig.hgetAll(RedisConfig.CACHE, ROLES));
				urls.putAll(RedisConfig.hgetAll(RedisConfig.CACHE, URLS));
			}catch(Exception e) {
				log.warn("fail to reload, ex: {} msg: {}", e.getClass().getSimpleName(), e.getMessage());
			}
		}
	}
	
}
