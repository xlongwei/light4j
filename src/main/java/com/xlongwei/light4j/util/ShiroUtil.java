package com.xlongwei.light4j.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.shiro.util.StringUtils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

/**
 * shiro util
 * @author xlongwei
 *
 */
public class ShiroUtil {
	/**{admin=123456,admin}*/
	public static final String USERS = "shiro.users";
	/**{admin=*:*:*}*/
	public static final String ROLES = "shiro.roles";
	/**
	 * /openapi/upload/*=roles["admin,user",admin],perms[openapi:upload:*,"service:upload:upload,delete"]
	 */
	public static final String URLS = "shiro.urls";
	
	/** 获取用户密码 */
	public static String getPassword(String username) {
		if(StrUtil.isNotBlank(username)) {
			String passwordRoles = RedisConfig.hget(RedisConfig.CACHE, USERS, username);
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
			String passwordRoles = RedisConfig.hget(RedisConfig.CACHE, USERS, username);
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
				String perms = RedisConfig.hget(RedisConfig.CACHE, ROLES, role);
				String[] permArray = StringUtils.split(perms, StringUtils.DEFAULT_DELIMITER_CHAR, '"', '"', true, true);
				for(String perm : permArray) {
					if(perm.indexOf('"') > -1) {
						perm = perm.substring(0, perm.length()-1);
					}
					permList.add(perm);
				}
			});
			return permList;
		}
		return Collections.emptySet();
	}
}
