package com.xlongwei.light4j.handler.service;

import java.util.List;
import java.util.Map;

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
		if(StringUtil.hasLength(idNumber)) {
			boolean valid = StringUtil.isIdNumber(idNumber);
			Map<String, String> params = StringUtil.params("valid", String.valueOf(valid));
			int areaMin = 2, areaYear = 10, areaBirth = 14;
			if(idNumber.length()>=areaMin) {
				String area = idNumber.substring(0,Math.min(idNumber.length(), 6));
				String areas = StringUtil.join(IdCardUtil.areas(area), null, null, null);
				params.put("area", area);
				params.put("areas", areas);
				if(idNumber.length()>=areaYear) {
					String year = idNumber.length() == 15 ? "19" + idNumber.substring(6,8) : idNumber.substring(6, 10);
					String month = idNumber.length() == 15 ? idNumber.substring(8, 10) : (idNumber.length()>=12 ? idNumber.substring(10,12) : "01");
					String day = idNumber.length() ==15 ? idNumber.substring(10, 12) : (idNumber.length()>=areaBirth ? idNumber.substring(12, 14) : "01");
					String serial = idNumber.length() ==15 ? idNumber.substring(12, 15) : (idNumber.length()==18 ? idNumber.substring(14, 17) : null);
					int age = IdCardUtil.age(year, month, day);
					params.put("age", String.valueOf(age));
					if(idNumber.length()>=areaBirth) {
						params.put("birth", year+month+day);
					}
					if(serial != null) {
						boolean male = Integer.parseInt(serial)%2==1;
						params.put("male", Boolean.toString(male));
						params.put("sex", male?"男":"女");
					}
				}
			}
			
			HandlerUtil.setResp(exchange, params);
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
