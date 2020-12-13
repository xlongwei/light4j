package com.xlongwei.light4j.handler.service;

import java.util.Collections;
import java.util.List;

import com.xlongwei.light4j.beetl.model.District;
import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.MySqlUtil;
import com.xlongwei.light4j.util.StringUtil;

import io.undertow.server.HttpServerExchange;

/**
 * district handler
 * @author xlongwei
 *
 */
public class DistrictHandler extends AbstractHandler {
	
	public void provinces(HttpServerExchange exchange) throws Exception {
		List<District> provinces = MySqlUtil.SQLMANAGER.lambdaQuery(District.class).distinct().groupBy("province").orderBy("provinceName").select("province", "provinceName");
		HandlerUtil.setResp(exchange, Collections.singletonMap("provinces", provinces));
	}
	
	public void cities(HttpServerExchange exchange) throws Exception {
		String province = HandlerUtil.getParam(exchange, "province");
		if(!StringUtil.isBlank(province)) {
			List<District> provinces = MySqlUtil.SQLMANAGER.lambdaQuery(District.class).distinct().andEq("province", province).groupBy("city").orderBy("cityName").select("city", "cityName");
			HandlerUtil.setResp(exchange, Collections.singletonMap("cities", provinces));
		}
	}
	
	public void counties(HttpServerExchange exchange) throws Exception {
		String city = HandlerUtil.getParam(exchange, "city");
		if(!StringUtil.isBlank(city)) {
			List<District> provinces = MySqlUtil.SQLMANAGER.lambdaQuery(District.class).distinct().andEq("city", city).groupBy("county").orderBy("countyName").select("county", "countyName");
			HandlerUtil.setResp(exchange, Collections.singletonMap("counties", provinces));
		}
	}
	
	public void county(HttpServerExchange exchange) throws Exception {
		String county = HandlerUtil.getParam(exchange, "county");
		if(!StringUtil.isBlank(county)) {
			District single = MySqlUtil.SQLMANAGER.lambdaQuery(District.class).andEq("county", county).single();
			HandlerUtil.setResp(exchange, Collections.singletonMap("county", single));
		}
	}
}
