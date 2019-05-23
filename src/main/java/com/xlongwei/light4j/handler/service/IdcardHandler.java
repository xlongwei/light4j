package com.xlongwei.light4j.handler.service;

import java.util.List;

import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.IdCardUtil;
import com.xlongwei.light4j.util.StringUtil;

import io.undertow.server.HttpServerExchange;

/**
 * idcard util
 * @author xlongwei
 *
 */
public class IdcardHandler extends AbstractHandler {

	public void parse(HttpServerExchange exchange) throws Exception {
		String idNumber = HandlerUtil.getParam(exchange, "idNumber");
		if(StringUtil.isIdNumber(idNumber)) {
			String area = idNumber.substring(0,6);
			String year = idNumber.length() == 15 ? "19" + idNumber.substring(6,8) : idNumber.substring(6, 10);
			String month = idNumber.length() == 15 ? idNumber.substring(8, 10) : idNumber.substring(10,12);
			String day = idNumber.length() ==15 ? idNumber.substring(10, 12) : idNumber.substring(12, 14);
			String serial = idNumber.length() ==15 ? idNumber.substring(12, 15) : idNumber.substring(14, 17);
			
			String areas = StringUtil.join(IdCardUtil.areas(area), null, null, null);
			int age = IdCardUtil.age(year, month, day);
			boolean male = Integer.parseInt(serial)%2==1;
			
			HandlerUtil.setResp(exchange, StringUtil.params("area", area, "birth", year+month+day, "male", Boolean.toString(male), "areas", areas, "age", String.valueOf(age), "sex", male?"男":"女"));
		}
	}
	
	public void valid(HttpServerExchange exchange) throws Exception {
		String idNumber = HandlerUtil.getParam(exchange, "idNumber");
		HandlerUtil.setResp(exchange, StringUtil.params("valid", Boolean.toString(StringUtil.isIdNumber(idNumber))));
	}
	
	public void area(HttpServerExchange exchange) throws Exception {
		String area = HandlerUtil.getParam(exchange, "area");
		List<String> areas = IdCardUtil.areas(area);
		if(areas.size() > 0) {
			HandlerUtil.setResp(exchange, StringUtil.params("area", StringUtil.join(IdCardUtil.areas(area), null, null, null)));
		}
	}

}
