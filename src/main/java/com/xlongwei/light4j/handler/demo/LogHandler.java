package com.xlongwei.light4j.handler.demo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.networknt.utility.StringUtils;
import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.HandlerUtil;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import cn.hutool.core.map.MapUtil;
import io.undertow.server.HttpServerExchange;
import lombok.extern.slf4j.Slf4j;

/**
 * log handler
 * @author xlongwei
 */
@Slf4j
public class LogHandler extends AbstractHandler {
	public static final String token = System.getProperty("token");
	
	public void log(HttpServerExchange exchange) throws Exception {
		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		String loggerName = HandlerUtil.getParam(exchange, "logger");
		List<ch.qos.logback.classic.Logger> loggers = null;
		if (StringUtils.isNotBlank(loggerName)) {
			ch.qos.logback.classic.Logger logger = lc.getLogger(loggerName);
			if (logger != null) {
				loggers = Arrays.asList(logger);
				String levelName = HandlerUtil.getParam(exchange, "level");
				if (StringUtils.isNotBlank(levelName)
						&& (StringUtils.isBlank(token) || token.equals(HandlerUtil.getParam(exchange, "token")))) {
					Level level = Level.toLevel(levelName, null);
					log.warn("change logger:{} level from:{} to:{}", logger.getName(), logger.getLevel(), level);
					logger.setLevel(level);
				}
			}
		}
		if (loggers == null) {
			loggers = lc.getLoggerList();
		}
		log.info("check logger level, loggers:{}", loggers.size());
		List<Map<String, String>> list = loggers.stream().sorted((a, b) -> a.getName().compareTo(b.getName()))
				.map(logger -> {
					Map<String, String> map = new HashMap<>(4);
					map.put("logger", logger.getName());
					map.put("level", Objects.toString(logger.getLevel(), ""));
					return map;
				}).collect(Collectors.toList());
		HandlerUtil.setResp(exchange, MapUtil.of("loggers", list));
	}
	
}
