package com.xlongwei.light4j.handler.service;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.ConfigUtil;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.StringUtil;

import cn.hutool.core.map.MapUtil;
import io.undertow.server.HttpServerExchange;
import lombok.extern.slf4j.Slf4j;

/**
 * mobile handler
 * @author xlongwei
 * @date 2019-10-30
 */
@Slf4j
public class MobileHandler extends AbstractHandler {

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		String mobile = HandlerUtil.getParam(exchange, "mobile");
		if(StringUtil.isNumbers(mobile)) {
			int type = StringUtil.getMobileType(mobile);
			Map<String, String> map = new HashMap<>(16);
			map.put("type", String.valueOf(type));
			map.put("valid", String.valueOf(type>=0 && mobile.length()==11));
			Map<String, String> searchToMap = searchToMap(mobile);
			if(MapUtil.isNotEmpty(searchToMap)) {
				map.putAll(searchToMap);
			}
			HandlerUtil.setResp(exchange, map);
		}
	}

	public static final String[] NUMBER_TYPE = { null, "移动", "联通", "电信", "电信虚拟运营商", "联通虚拟运营商", "移动虚拟运营商" };
	private static final int INDEX_SEGMENT_LENGTH = 9;

	private static final byte[] DATA_BYTE_ARRAY;
	private static final ByteBuffer BYTE_BUFFER;
	private static final int INDEX_AREA_OFFSET;
	private static final int PHONE_RECORD_COUNT;
	private static final int PHONE_MAX_LENGTH = 11, PHONE_REGION_LENGTH = 7;

	static {
		ByteArrayOutputStream byteData = new ByteArrayOutputStream();

		try(InputStream inputStream = new BufferedInputStream(ConfigUtil.stream("phone.dat"))) {
			IOUtils.copy(inputStream, byteData);
		} catch (Exception e) {
			log.warn("fail to init phone.dat, ex: {}", e.getMessage());
		}

		DATA_BYTE_ARRAY = byteData.toByteArray();

		BYTE_BUFFER = ByteBuffer.wrap(DATA_BYTE_ARRAY);
		BYTE_BUFFER.order(ByteOrder.LITTLE_ENDIAN);
		int dataVersion = BYTE_BUFFER.getInt();
		INDEX_AREA_OFFSET = BYTE_BUFFER.getInt();

		PHONE_RECORD_COUNT = (DATA_BYTE_ARRAY.length - INDEX_AREA_OFFSET) / INDEX_SEGMENT_LENGTH;
		log.info("phone.dat loaded, dataVersion={}, recordCount={}", dataVersion, PHONE_RECORD_COUNT);
	}

	/**
	 * 查找手机号归属信息
	 * @param phoneNumber
	 * @return null 或 province|city|zipCode|areaCode|type
	 */
	public static synchronized String search(String phoneNumber) {
		if (phoneNumber == null || phoneNumber.length() > PHONE_MAX_LENGTH || phoneNumber.length() < PHONE_REGION_LENGTH || PHONE_RECORD_COUNT < 1) {
			return null;
		}
		int phoneNumberPrefix;
		try {
			phoneNumberPrefix = Integer.parseInt(phoneNumber.substring(0, 7));
		} catch (Exception e) {
			return null;
		}
		int left = 0;
		int right = PHONE_RECORD_COUNT;
		while (left <= right) {
			int middle = (left + right) >> 1;
			int currentOffset = INDEX_AREA_OFFSET + middle * INDEX_SEGMENT_LENGTH;
			if (currentOffset >= DATA_BYTE_ARRAY.length) {
				return null;
			}

			BYTE_BUFFER.position(currentOffset);
			int currentPrefix = BYTE_BUFFER.getInt();
			if (currentPrefix > phoneNumberPrefix) {
				right = middle - 1;
			} else if (currentPrefix < phoneNumberPrefix) {
				left = middle + 1;
			} else {
				int infoBeginOffset = BYTE_BUFFER.getInt();
				int phoneType = BYTE_BUFFER.get();

				int infoLength = -1;
				for (int i = infoBeginOffset; i < INDEX_AREA_OFFSET; ++i) {
					if (DATA_BYTE_ARRAY[i] == 0) {
						infoLength = i - infoBeginOffset;
						break;
					}
				}

				String infoString = new String(DATA_BYTE_ARRAY, infoBeginOffset, infoLength, StandardCharsets.UTF_8);
				return infoString+"|"+phoneType;
			}
		}
		return null;
	}
	
	public static synchronized Map<String, String> searchToMap(String mobile) {
		String infoString = search(mobile);
		if(infoString != null) {
			String[] infoSegments = infoString.split("\\|");
			Map<String, String> map = MapUtil.newHashMap();
			map.put("province", infoSegments[0]);
			map.put("city", infoSegments[1]);
			map.put("zipCode", infoSegments[2]);
			map.put("areaCode", infoSegments[3]);
			map.put("isp", NUMBER_TYPE[Integer.parseInt(infoSegments[4])]);
			map.put("region", map.get("province")+map.get("city")+map.get("isp"));
			return map;
		}
		return Collections.emptyMap();
	}
}
