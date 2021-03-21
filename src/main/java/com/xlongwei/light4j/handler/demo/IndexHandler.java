package com.xlongwei.light4j.handler.demo;

import java.util.Collections;
import java.util.Map;

import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;

import org.beetl.sql.core.SQLReady;

import com.xlongwei.light4j.beetl.dao.UserDao;
import com.xlongwei.light4j.beetl.model.User;
import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.handler.service.IpHandler;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.JsonUtil;
import com.xlongwei.light4j.util.MySqlUtil;
import com.xlongwei.light4j.util.RedisConfig;
import com.xlongwei.light4j.util.StringUtil;

import cn.hutool.script.ScriptUtil;
import io.undertow.server.HttpServerExchange;
import lombok.extern.slf4j.Slf4j;

/**
 * beetl index demo
 * @author xlongwei
 *
 */
@Slf4j
public class IndexHandler extends AbstractHandler {

	public void index(HttpServerExchange exchange) throws Exception {
		String ip = HandlerUtil.getIp(exchange);
		String region = IpHandler.searchToMap(ip).get("region");
		HandlerUtil.setResp(exchange, StringUtil.params("ip", ip, "region", region));
	}
	
	public void mysql(HttpServerExchange exchange) throws Exception {
		String type = StringUtil.firstNotBlank(HandlerUtil.getParam(exchange, "type"), "tables");
		Object obj = null;
		switch(type) {
		case "all": obj = MySqlUtil.SQLMANAGER.all(User.class); break;
//		case "sql": obj = MySqlUtil.SQLMANAGER.select("user.sample", User.class); break;
		case "dao": obj = MySqlUtil.SQLMANAGER.getMapper(UserDao.class).all(); break;
		default: obj = MySqlUtil.SQLMANAGER.getMetaDataManager().allTable(); break;
		}
		HandlerUtil.setResp(exchange, Collections.singletonMap("allTable", obj));
	}
	
	public void script(HttpServerExchange exchange) throws Exception {
		String script = HandlerUtil.getParam(exchange, "script");
		if(!StringUtil.isBlank(script)) {
			ScriptContext context = new SimpleScriptContext();
			context.setAttribute("log", log, ScriptContext.ENGINE_SCOPE);
			context.setAttribute("util", new Util(), ScriptContext.ENGINE_SCOPE);
			ScriptUtil.eval(script, context);
		}
		HandlerUtil.setResp(exchange, exchange.getAttachment(HandlerUtil.BODY));
	}
	
	public static class Util {
		public Map<String, String> hgetall(String key) {
			return StringUtil.isBlank(key) ? Collections.emptyMap() : RedisConfig.hgetAll(RedisConfig.CACHE, key);
		}
		public String get(String json, String path) {
			return StringUtil.isBlank(json) || StringUtil.isBlank(path) ? "" : JsonUtil.get(json, path);
		}
		public void insert(String sql, Object ... args) {
			MySqlUtil.SQLMANAGER.executeUpdate(new SQLReady(sql, args));
		}
	}
}
