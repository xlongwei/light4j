package com.xlongwei.light4j.handler.demo;

import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
	
	public void log(HttpServerExchange exchange) throws Exception {
		Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();
		Deque<String> loggerParams = queryParameters.get("logger");
		List<ch.qos.logback.classic.Logger> loggers = null;
		if(loggerParams!=null && loggerParams.size()>0) {
			Set<String> loggerNames = loggerParams.stream().map(loggerName -> StringUtils.trimToEmpty(loggerName)).collect(Collectors.toSet());
			if(!loggerNames.isEmpty()) {
				loggers = loggerNames.stream().map(loggerName -> (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(loggerName)).filter(logger -> logger!=null).collect(Collectors.toList());
				if(!loggers.isEmpty()) {
					Deque<String> levelParams = queryParameters.get("level");
					if(levelParams!=null && levelParams.size()>0) {
						String levelName = levelParams.getFirst();
						Level level = Level.toLevel(levelName, null);
						loggers.forEach(logger -> {
							log.info("change logger:{} level from:{} to:{}", logger.getName(), logger.getLevel(), level);
							logger.setLevel(level);
						});
					}
				}
			}
		}
		if(loggers == null) {
			LoggerContext lc = (LoggerContext)LoggerFactory.getILoggerFactory();
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
