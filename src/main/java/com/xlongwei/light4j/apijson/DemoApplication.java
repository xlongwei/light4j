package com.xlongwei.light4j.apijson;

import java.util.Map;
import java.util.regex.Pattern;

import com.xlongwei.light4j.util.NumberUtil;

import apijson.Log;
import apijson.StringUtil;
import apijson.framework.APIJSONApplication;
import apijson.framework.APIJSONCreator;
import apijson.orm.FunctionParser;
import apijson.orm.Parser;
import apijson.orm.SQLConfig;
import apijson.orm.SQLExecutor;
import apijson.orm.Structure;
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
	public static final boolean apijsonEnabled = NumberUtil.parseBoolean(System.getProperty("apijson.enabled"), true);
	public static final DemoController apijson = new DemoController();
	
	public static void start() {
		if(apijsonEnabled) {
			Map<String, Pattern> COMPILE_MAP = Structure.COMPILE_MAP;
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
