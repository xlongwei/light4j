package com.xlongwei.light4j.openapi.extend;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.cache.MemoryConstrainedCacheManager;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.DefaultSessionStorageEvaluator;
import org.apache.shiro.mgt.DefaultSubjectDAO;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.SubjectContext;
import org.apache.shiro.util.AntPathMatcher;
import org.apache.shiro.util.CollectionUtils;
import org.apache.shiro.util.StringUtils;
import org.jose4j.jwt.JwtClaims;

import com.networknt.openapi.JwtVerifyHandler;
import com.networknt.security.JwtVerifier;
import com.networknt.utility.Constants;
import com.xlongwei.light4j.util.RedisPubsub;
import com.xlongwei.light4j.util.ShiroUtil;
import com.xlongwei.light4j.util.StringUtil;
import com.xlongwei.light4j.util.RedisPubsub.MessageListener;

import cn.hutool.core.util.CharUtil;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import lombok.extern.slf4j.Slf4j;

/**
 * 在jwt token的基础上扩展权限校验功能
 * @author xlongwei
 *
 */
@Slf4j
public class MyJwtShiroHandler extends DummyMiddlewareHandler {
	
	public MyJwtShiroHandler() {
		DefaultSecurityManager securityManager = new DefaultSecurityManager() {
			@Override
			protected SubjectContext createSubjectContext() {
				SubjectContext subjectContext = super.createSubjectContext();
				subjectContext.setSessionCreationEnabled(false);
				return subjectContext;
			}
		};
		DefaultSubjectDAO subjectDao = (DefaultSubjectDAO)securityManager.getSubjectDAO();
		DefaultSessionStorageEvaluator dsse = (DefaultSessionStorageEvaluator)subjectDao.getSessionStorageEvaluator();
		dsse.setSessionStorageEnabled(false);
		
		SecurityUtils.setSecurityManager(securityManager);
		securityManager.setRealm(new JwtAuthenticationRealm());
		securityManager.setCacheManager(new MemoryConstrainedCacheManager());
		ShiroUtil.reload(false);
		RedisPubsub.sub(new MessageListener() {
			@Override
			public void onMessage(String message) {
				if(ShiroUtil.MESSAGE.equals(message)) {
					ShiroUtil.reload(false);
				}
			}
		});
	}
	
	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
        String authorization = exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION);
        String jwt = JwtVerifier.getJwtFromAuthorization(authorization);
        Subject subject = SecurityUtils.getSubject();
        subject.login(new JwtAuthenticationToken(jwt));
        try{
        	String path = exchange.getRequestPath();
        	if(path.lastIndexOf(CharUtil.DOT) > -1) {
        		path = path.substring(0, path.lastIndexOf(CharUtil.DOT));
        	}
        	Map<String, String> urls = ShiroUtil.getUrls();
        	checkUrls(subject, path, urls);
        	super.handleRequest(exchange);
        }catch(AuthorizationException e) {
        	setExchangeStatus(exchange, "ERR12007");
        }finally {
        	subject.logout();
		}
	}
	
	private void checkUrls(Subject subject, String path, Map<String, String> urls) {
		if(StringUtil.isBlank(path) || CollectionUtils.isEmpty(urls)) {
			return;
		}
		AntPathMatcher antPathMatcher = new AntPathMatcher();
		urls.forEach((antPath, rolesPermsList) -> {
			if(antPathMatcher.matchStart(antPath, path)) {
				//roles[admin,user], perms[file:edit]
				log.debug("path: {}, matcher: {}, rolesPerms: {}", path, antPath, rolesPermsList);
				String[] rolesPermsArray = StringUtils.split(rolesPermsList, StringUtils.DEFAULT_DELIMITER_CHAR, '[', ']', true, true);
				for (String rolesPerms : rolesPermsArray) {
		            checkRolesPerms(subject, rolesPerms);
				}
			}
		});
	}
	
	private void checkRolesPerms(Subject subject, String rolesPerms) {
		//roles["admin,user","client"] 校验角色：(admin and user) or client
		String roles = StringUtil.getPatternString(rolesPerms, "roles\\[(.*)\\]");
		if(StringUtil.hasLength(roles)) {
			String[] roleArray = StringUtils.split(roles, StringUtils.DEFAULT_DELIMITER_CHAR, CharUtil.DOUBLE_QUOTES, CharUtil.DOUBLE_QUOTES, true, true);
			if(roleArray.length > 1) {
				for(String role : roleArray) {
					log.debug("checkRole: {}", role);
					boolean hasRole = role.indexOf(CharUtil.DOUBLE_QUOTES)==-1 && subject.hasRole(role);
					boolean hasAllRoles = role.indexOf(CharUtil.DOUBLE_QUOTES) > -1 && subject.hasAllRoles(Arrays.asList(role.substring(1, role.length()-1).split("[,]")));
					if(hasRole || hasAllRoles) {
						return;
					}
				}
				throw new AuthorizationException();
			}else {
				String role = roleArray[0];
				log.debug("checkRole: {}", role);
				if(role.indexOf(CharUtil.DOUBLE_QUOTES) == -1) {
					subject.checkRole(role);
				}else {
					subject.checkRoles(role.substring(0, role.length()-1).split(","));
				}
				return;
			}
		}
		//perms[openapi:upload:*,"service:upload:upload,temp"] 校验权限：openapi:upload:* or service:upload:upload,temp
		String perms = StringUtil.getPatternString(rolesPerms, "perms\\[(.*)\\]");
		if(StringUtil.hasLength(perms)) {
			String[] permArray = StringUtils.split(perms, StringUtils.DEFAULT_DELIMITER_CHAR, CharUtil.DOUBLE_QUOTES, CharUtil.DOUBLE_QUOTES, true, true);
			if(permArray.length > 1) {
				for(String perm : permArray) {
					log.debug("checkPerm: {}", perm);
					if(perm.indexOf(CharUtil.DOUBLE_QUOTES) > -1) {
						perm = perm.substring(1, perm.length() -1);
					}
					if(subject.isPermitted(perm)) {
						return;
					}
				}
				throw new AuthorizationException();
			}else {
				String perm = permArray[0];
				log.debug("checkPerm: {}", perm);
				if(perm.indexOf(CharUtil.DOUBLE_QUOTES) > -1) {
					perm = perm.substring(0, perm.length()-1);
				}
				subject.checkPermission(perm);
				return;
			}
		}
	}
	
	/** 使用jwt token认证 */
	public static class JwtAuthenticationToken implements AuthenticationToken {
		private static final long serialVersionUID = 2084281951776671254L;
		private String token;
		public JwtAuthenticationToken(String token) {
			this.token = token;
		}
		@Override
		public Object getPrincipal() {
			return token;
		}
		@Override
		public Object getCredentials() {
			return token;
		}
	}
	
	/** 通过jwt token里面的user_id获取角色和权限 */
	public static class JwtAuthenticationRealm extends AuthorizingRealm {
		@Override
		public boolean supports(AuthenticationToken token) {
			return token!=null && token.getClass()==JwtAuthenticationToken.class;
		}
		@Override
		protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
			SimpleAuthorizationInfo authz = new SimpleAuthorizationInfo();
	        try{
	        	String jwt = (String)principals.getPrimaryPrincipal();
	        	JwtClaims claims = JwtVerifyHandler.jwtVerifier.verifyJwt(jwt, true, true);
	        	String userId = claims.getStringClaimValue(Constants.USER_ID_STRING);
	        	//这里可以自定义用户的角色和权限列表
	        	Set<String> roles = ShiroUtil.getRoles(userId);
	        	Set<String> perms = ShiroUtil.getPerms(roles);
	        	authz.setRoles(roles);
	        	authz.setStringPermissions(perms);
	        	log.debug("userId: {}, roles: {}, perms: {}", userId, authz.getRoles(), authz.getStringPermissions());
	        }catch(Exception e) {
	        	log.info("authorize ex: {}, msg: {}", e.getClass().getSimpleName(), e.getMessage());
	        }
			return authz;
		}
		@Override
		protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
			return new SimpleAuthenticationInfo(token.getPrincipal(), token.getCredentials(), getName());
		}
	}
}
