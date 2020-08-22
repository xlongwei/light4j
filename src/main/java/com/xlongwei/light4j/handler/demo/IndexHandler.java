package com.xlongwei.light4j.handler.demo;

import java.util.Collections;

import com.xlongwei.light4j.beetl.dao.UserDao;
import com.xlongwei.light4j.beetl.model.User;
import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.handler.service.IpHandler;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.MySqlUtil;
import com.xlongwei.light4j.util.StringUtil;

import io.undertow.server.HttpServerExchange;

/**
 * beetl index demo
 * @author xlongwei
 *
 */
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
		case "sql": obj = MySqlUtil.SQLMANAGER.select("user.sample", User.class); break;
		case "dao": obj = MySqlUtil.SQLMANAGER.getMapper(UserDao.class).all(); break;
		default: obj = MySqlUtil.SQLMANAGER.getMetaDataManager().allTable(); break;
		}
		HandlerUtil.setResp(exchange, Collections.singletonMap("allTable", obj));
	}
	
}
