package com.xlongwei.light4j.handler.service;

import java.util.HashMap;
import java.util.Map;

import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.ImageUtil;
import com.xlongwei.light4j.util.RedisCache;
import com.xlongwei.light4j.util.StringUtil;

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
			boolean valid = false;
			switch(type) {
			case "numbers": valid = StringUtil.isNumbers(value); break;
			case "decimal": valid = StringUtil.isDecimal(value); break;
			case "money": valid = StringUtil.isMoney(value); break;
			case "identifier": valid = StringUtil.isIdentifier(value); break;
			case "chinese": valid = StringUtil.isChinese(value); break;
			case "email": valid = StringUtil.isEmail(value); break;
			case "tel": valid = StringUtil.isTel(value); break;
			case "mobile": valid = StringUtil.isMobile(value); map.put("type", String.valueOf(StringUtil.getMobileType(value))); break;
			case "barcode": valid = StringUtil.isBarcode(value); break;
			case "ip": valid = StringUtil.isIp(value); break;
			case "url": valid = StringUtil.isUrl(value); break;
			case "idNumber": valid = StringUtil.isIdNumber(value); break;
			case "sccNumber": valid = StringUtil.isSccNumber(value.toUpperCase()); break;
			case "businessNo": valid = StringUtil.isBusinessNo(value); break;
			case "organizationCode": valid = StringUtil.isOrganizationCode(value); break;
			case "taxRegistrationNo": valid = StringUtil.isTaxRegistrationNo(value); break;
			case "plateNumber": valid = StringUtil.isPlateNumber(value); break;
			case "bankCardNumber": valid = StringUtil.isBankCardNumber(value); break;
			case "checkcode": {
				String sid = HandlerUtil.getParam(exchange, "sid");
				String imgCode = RedisCache.get(ImageUtil.attr, sid);
				valid = value.equalsIgnoreCase(imgCode); break;
			}
			default: if(type.startsWith(SLASH) && type.endsWith(SLASH)) { valid = value.matches(type.substring(1, type.length()-1)); } break;
			}
			map.put("valid", String.valueOf(valid));
		}else {
			map.put("valid", "true");
		}
		HandlerUtil.setResp(exchange, map);
	}

}
