package com.xlongwei.light4j.apijson;

import java.util.Map;
import java.util.regex.Pattern;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.xlongwei.light4j.util.NumberUtil;

import apijson.Log;
import apijson.StringUtil;
import apijson.framework.APIJSONApplication;
import apijson.framework.APIJSONCreator;
import apijson.orm.AbstractVerifier;
import apijson.orm.FunctionParser;
import apijson.orm.Parser;
import apijson.orm.SQLConfig;
import apijson.orm.SQLExecutor;
import apijson.orm.Verifier;
import lombok.extern.slf4j.Slf4j;

/**
 * <li>apijson.enabled=false停用apijson
 * <li>apijson.debug=true开启调试
 * @author xlongwei
 * @see https://gitee.com/APIJSON/APIJSON.git
 */
@Slf4j
public class DemoApplication {
	public static final boolean apijsonEnabled = NumberUtil.parseBoolean(System.getProperty("apijson.enabled"), false);
	public static final DemoController apijson = new DemoController();
	
	public static void start() {
		if (apijsonEnabled) {
			//JSON.getCorrectJson调用JSON.parse未加有序特性时造成bug，可以考虑JSON默认有序
			JSON.DEFAULT_PARSER_FEATURE |= Feature.OrderedField.getMask();
			//Verifier校验参数规则Operation.VERIFY时支持预定义正则PHONE EMAIL ID_CARD等
			Map<String, Pattern> COMPILE_MAP = AbstractVerifier.COMPILE_MAP;
			COMPILE_MAP.put("PHONE", StringUtil.PATTERN_PHONE);
			COMPILE_MAP.put("EMAIL", StringUtil.PATTERN_EMAIL);
			COMPILE_MAP.put("ID_CARD", StringUtil.PATTERN_ID_CARD);
			
			APIJSONApplication.DEFAULT_APIJSON_CREATOR = new APIJSONCreator() {
				@Override
				public Parser<Long> createParser() {
					return new DemoParser();
				}
				@Override
				public FunctionParser createFunctionParser() {
					return new DemoFunctionParser();
				}
				@Override
				public Verifier<Long> createVerifier() {
					return new DemoVerifier();
				}
				@Override
				public SQLConfig createSQLConfig() {
					return new DemoSQLConfig();
				}
				@Override
				public SQLExecutor createSQLExecutor() {
					return new DemoSQLExecutor();
				}
			};
			Log.DEBUG = NumberUtil.parseBoolean(System.getProperty("apijson.debug"), false);
			try{
				APIJSONApplication.init(false);
			}catch(Exception e) {
				log.warn("fail to start apijson: {}", e.getMessage());
			}
		}
		log.info("apijsonEnabled={}", apijsonEnabled);
	}
}
