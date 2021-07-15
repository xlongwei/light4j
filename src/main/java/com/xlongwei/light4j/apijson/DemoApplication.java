package com.xlongwei.light4j.apijson;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.serializer.PropertyFilter;
import com.networknt.service.SingletonServiceFactory;
import com.xlongwei.light4j.util.NumberUtil;

import apijson.Log;
import apijson.NotNull;
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
import unitauto.MethodUtil;
import unitauto.MethodUtil.Argument;
import unitauto.MethodUtil.InstanceGetter;
import unitauto.MethodUtil.JSONCallback;
import unitauto.jar.UnitAutoApp;

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
			
			unitauto();
			
			try{
				APIJSONApplication.init(false);
			}catch(Exception e) {
				log.warn("fail to start apijson: {}", e.getMessage());
			}
		}
		log.info("apijsonEnabled={}", apijsonEnabled);
	}

	private static void unitauto() {
		UnitAutoApp.init();
			final InstanceGetter ig = MethodUtil.INSTANCE_GETTER;
			MethodUtil.INSTANCE_GETTER = new InstanceGetter() {
				@Override
				public Object getInstance(@NotNull Class<?> clazz, List<Argument> classArgs, Boolean reuse) throws Exception {
					if (classArgs == null || classArgs.isEmpty()) {
						Object singleton = SingletonServiceFactory.getBean(clazz);
						if(singleton!=null){
							return singleton;
						}
					}
					return ig.getInstance(clazz, classArgs, reuse);
				}
			};
			final JSONCallback jc = MethodUtil.JSON_CALLBACK;
			MethodUtil.JSON_CALLBACK = new JSONCallback() {
	
				@Override
				public JSONObject newSuccessResult() {
					return jc.newSuccessResult();
				}
	
				@Override
				public JSONObject newErrorResult(Throwable e) {
					return jc.newErrorResult(e);
				}
	
				@Override
				public JSONObject parseJSON(String type, Object value) {
					if (value == null || unitauto.JSON.isBooleanOrNumberOrString(value) || value instanceof JSON || value instanceof Enum) {
						return jc.parseJSON(type, value);
					}
	
					if (/*value instanceof ApplicationContext
							|| value instanceof Context
							|| value instanceof org.apache.catalina.Context
							|| */value instanceof ch.qos.logback.core.Context
							) {
						value = value.toString();
					}
					else {
						try {
							value = JSON.parse(JSON.toJSONString(value, new PropertyFilter() {
								@Override
								public boolean apply(Object object, String name, Object value) {
									if (value == null) {
										return true;
									}
	
									if (/*value instanceof ApplicationContext
											|| value instanceof Context
											|| value instanceof org.apache.catalina.Context
											||*/ value instanceof ch.qos.logback.core.Context
											) {
										return false;
									}
	
									// 防止通过 UnitAuto 远程执行 getDBPassword 等方法来查到敏感信息，但如果直接调用 public String getDBUri 这里没法拦截，仍然会返回敏感信息
									//	if (object instanceof SQLConfig) {
									//		// 这个类部分方法不序列化返回					
									//		if ("dBUri".equalsIgnoreCase(name) || "dBPassword".equalsIgnoreCase(name) || "dBAccount".equalsIgnoreCase(name)) {
									//			return false;
									//		}
									//		return false;  // 这个类所有方法都不序列化返回
									//	}
									
									// 所有类中的方法只要包含关键词就不序列化返回
									String n = StringUtil.toLowerCase(name);
									if (n.contains("database") || n.contains("schema") || n.contains("dburi") || n.contains("password") || n.contains("account")) {
										return false;
									}
	
									return Modifier.isPublic(value.getClass().getModifiers());
								}
							}));
						} catch (Exception e) {
							log.warn("toJSONString  catch \n" + e.getMessage());
						}
					}
	
					return jc.parseJSON(type, value);
				}
	
			};
	}
}
