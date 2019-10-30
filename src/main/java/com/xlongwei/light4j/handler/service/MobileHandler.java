package com.xlongwei.light4j.handler.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.ConfigUtil;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.StringUtil;

import io.undertow.server.HttpServerExchange;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MobileHandler extends AbstractHandler {

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		String mobile = HandlerUtil.getParam(exchange, "mobile");
		if(StringUtil.isNumbers(mobile)) {
			int type = StringUtil.getMobileType(mobile);
			Map<String, String> map = new HashMap<>(4);
			map.put("type", String.valueOf(type));
			map.put("valid", String.valueOf(type>=0 && mobile.length()==11));
			String infoString = search(mobile);
			if(infoString != null) {
				String[] infoSegments = infoString.split("\\|");
				map.put("province", infoSegments[0]);
				map.put("city", infoSegments[1]);
				map.put("zipCode", infoSegments[2]);
				map.put("areaCode", infoSegments[3]);
				map.put("isp", numberType[Integer.parseInt(infoSegments[4])]);
			}
			HandlerUtil.setResp(exchange, map);
		}
	}

	public static final String[] numberType = { null, "移动", "联通", "电信", "电信虚拟运营商", "联通虚拟运营商", "移动虚拟运营商" };
	private static final int INDEX_SEGMENT_LENGTH = 9;

	private static final byte[] dataByteArray;
	private static final ByteBuffer byteBuffer;
	private static final int indexAreaOffset;
	private static final int phoneRecordCount;

	static {
		ByteArrayOutputStream byteData = new ByteArrayOutputStream();

		try(InputStream inputStream = ConfigUtil.stream("phone.dat")) {
			IOUtils.copy(inputStream, byteData);
		} catch (Exception e) {
			log.warn("fail to init phone.dat, ex: {}", e.getMessage());
		}

		dataByteArray = byteData.toByteArray();

		byteBuffer = ByteBuffer.wrap(dataByteArray);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		int dataVersion = byteBuffer.getInt();
		indexAreaOffset = byteBuffer.getInt();

		phoneRecordCount = (dataByteArray.length - indexAreaOffset) / INDEX_SEGMENT_LENGTH;
		log.info("phone.dat loaded, dataVersion={}, recordCount={}", dataVersion, phoneRecordCount);
	}

	/**
	 * 查找手机号归属信息
	 * @param phoneNumber
	 * @return null 或 province|city|zipCode|areaCode|type
	 */
	public static synchronized String search(String phoneNumber) {
		if (phoneNumber == null || phoneNumber.length() > 11 || phoneNumber.length() < 7 || phoneRecordCount < 1) {
			return null;
		}
		int phoneNumberPrefix;
		try {
			phoneNumberPrefix = Integer.parseInt(phoneNumber.substring(0, 7));
		} catch (Exception e) {
			return null;
		}
		int left = 0;
		int right = phoneRecordCount;
		while (left <= right) {
			int middle = (left + right) >> 1;
			int currentOffset = indexAreaOffset + middle * INDEX_SEGMENT_LENGTH;
			if (currentOffset >= dataByteArray.length) {
				return null;
			}

			byteBuffer.position(currentOffset);
			int currentPrefix = byteBuffer.getInt();
			if (currentPrefix > phoneNumberPrefix) {
				right = middle - 1;
			} else if (currentPrefix < phoneNumberPrefix) {
				left = middle + 1;
			} else {
				int infoBeginOffset = byteBuffer.getInt();
				int phoneType = byteBuffer.get();

				int infoLength = -1;
				for (int i = infoBeginOffset; i < indexAreaOffset; ++i) {
					if (dataByteArray[i] == 0) {
						infoLength = i - infoBeginOffset;
						break;
					}
				}

				String infoString = new String(dataByteArray, infoBeginOffset, infoLength, StandardCharsets.UTF_8);
				return infoString+"|"+phoneType;
			}
		}
		return null;
	}
}
