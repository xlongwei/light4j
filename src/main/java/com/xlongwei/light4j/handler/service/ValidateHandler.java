package com.xlongwei.light4j.handler.service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.DateUtil;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.HolidayUtil;
import com.xlongwei.light4j.util.IdCardUtil;
import com.xlongwei.light4j.util.ImageUtil;
import com.xlongwei.light4j.util.NumberUtil;
import com.xlongwei.light4j.util.PlateUtil;
import com.xlongwei.light4j.util.RedisCache;
import com.xlongwei.light4j.util.StringUtil;

import cn.hutool.core.map.MapUtil;
import io.undertow.server.HttpServerExchange;

/**
 * 校验常见表单数据
 * @author xlongwei
 *
 */
public class ValidateHandler extends AbstractHandler {

	private static final String SLASH = "/";

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		String type = HandlerUtil.getParam(exchange, "type");
		String value = HandlerUtil.getParam(exchange, "value");
		if(StringUtil.isBlank(type)) {
			return;
		}
		Map<String, String> map = new HashMap<>(2);
		if(!StringUtil.isBlank(value)) {
			validate(exchange, type, value, map);
		}else {
			map.put("valid", "true");
		}
		HandlerUtil.setResp(exchange, map);
	}

	private void validate(HttpServerExchange exchange, String type, String value, Map<String, String> map) {
		boolean valid = false;
		switch(type) {
		case "numbers": valid = StringUtil.isNumbers(value); break;
		case "decimal": valid = StringUtil.isDecimal(value); break;
		case "money": valid = money(value, map); break;
		case "identifier": valid = StringUtil.isIdentifier(value); break;
		case "chinese": valid = StringUtil.isChinese(value); break;
		case "email": valid = StringUtil.isEmail(value); break;
		case "tel": valid = StringUtil.isTel(value); break;
		case "mobile": valid = mobile(value, map); break;
		case "barcode": valid = StringUtil.isBarcode(value); break;
		case "ip": valid = ip(value, map); break;
		case "url": valid = StringUtil.isUrl(value); break;
		case "idArea": valid = IdCardUtil.areas.containsKey(value); map.put("area", StringUtil.join(IdCardUtil.areas(value), null, null, null)); break;
		case "idNumber": valid = idNumber(value, map); break;
		case "sccNumber": valid = StringUtil.isSccNumber(value.toUpperCase()); break;
		case "businessNo": valid = StringUtil.isBusinessNo(value); break;
		case "organizationCode": valid = StringUtil.isOrganizationCode(value); break;
		case "taxRegistrationNo": valid = StringUtil.isTaxRegistrationNo(value); break;
		case "plateNumber": valid = plateNumber(value, map); break;
		case "bankCardNumber": valid = StringUtil.isBankCardNumber(value); break;
		case "checkcode": valid = checkcode(exchange, value); break;
		case "workday": { Date day = DateUtil.parse(value); valid = day!=null && HolidayUtil.isworkday(day); } break;
		case "holiday": { Date day = DateUtil.parse(value); valid = day!=null && HolidayUtil.isholiday(day); } break;
		case "weekend": { Date day = DateUtil.parse(value); valid = day!=null && HolidayUtil.isweekend(day); } break;
		default: if(type.startsWith(SLASH) && type.endsWith(SLASH)) { valid = value.matches(type.substring(1, type.length()-1)); } break;
		}
		map.put("valid", String.valueOf(valid));
	}

	private boolean checkcode(HttpServerExchange exchange, String value) {
		boolean valid;
		String sid = HandlerUtil.getParam(exchange, "sid");
		String imgCode = RedisCache.get(ImageUtil.attr, sid);
		valid = value.equalsIgnoreCase(imgCode);
		return valid;
	}

	private boolean plateNumber(String value, Map<String, String> map) {
		boolean valid;
		valid = StringUtil.isPlateNumber(value);
		String text = PlateUtil.search(value);
		if(text != null) {
			map.put("text", text);
		}
		return valid;
	}

	private boolean idNumber(String value, Map<String, String> map) {
		boolean valid;
		valid = StringUtil.isIdNumber(value);
		int areaMin = 2, areaYear = 10, areaBirth = 14;
		if(value.length()>=areaMin) {
			String area = value.substring(0,Math.min(value.length(), 6));
			String areas = StringUtil.join(IdCardUtil.areas(area), null, null, null);
			map.put("area", areas);
			if(value.length()>=areaYear) {
				String year = value.length() == 15 ? "19" + value.substring(6,8) : value.substring(6, 10);
				String month = value.length() == 15 ? value.substring(8, 10) : (value.length()>=12 ? value.substring(10,12) : "01");
				String day = value.length() ==15 ? value.substring(10, 12) : (value.length()>=areaBirth ? value.substring(12, 14) : "01");
				String serial = value.length() ==15 ? value.substring(12, 15) : (value.length()==18 ? value.substring(14, 17) : null);
				int age = IdCardUtil.age(year, month, day);
				map.put("age", String.valueOf(age));
				if(value.length()>=areaBirth) {
					map.put("birth", year+month+day);
				}
				if(serial != null) {
					boolean male = Integer.parseInt(serial)%2==1;
					map.put("male", Boolean.toString(male));
					map.put("sex", male?"男":"女");
				}
			}
		}
		return valid;
	}

	private boolean ip(String value, Map<String, String> map) {
		boolean valid;
		valid = StringUtil.isIp(value);
		if(valid) {
			Map<String, String> searchToMap = IpHandler.searchToMap(value);
			if(MapUtil.isNotEmpty(searchToMap)) {
				map.putAll(searchToMap);
			}
		}
		return valid;
	}

	private boolean mobile(String value, Map<String, String> map) {
		boolean valid;
		valid = StringUtil.isMobile(value);
		map.put("type", String.valueOf(StringUtil.getMobileType(value)));
		Map<String, String> searchToMap = MobileHandler.searchToMap(value);
		if(MapUtil.isNotEmpty(searchToMap)) {
			map.putAll(searchToMap);
		}
		return valid;
	}

	private boolean money(String value, Map<String, String> map) {
		boolean valid;
		valid = StringUtil.isMoney(value);
		if(valid) {
			String daxie = NumberUtil.daxie(value);
			map.put("daxie", daxie);
		}
		return valid;
	}

}
