package com.xlongwei.light4j.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.dbutils.QueryRunner;

import com.networknt.config.Config;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import lombok.extern.slf4j.Slf4j;

/**
 * mysql util
 * @author xlongwei
 */
@Slf4j
@SuppressWarnings({"rawtypes", "unchecked"})
public class MySqlUtil {
	public static final Map<String, HikariDataSource> DATASOURCEMAP = new HashMap<>();
	public static final HikariDataSource DATASOURCE;
	public static final QueryRunner QUERYRUNNER;
	
	static {
		Map<String, Object> dataSourceMap = (Map<String, Object>) Config.getInstance().getJsonMapConfig("mysql");
		dataSourceMap.forEach((k, v) -> {
			Properties props = new Properties();
			Map configs = new HashMap((Map)v);
			Map<String, ?> params = (Map)configs.remove("parameters");
            params.forEach((p, q) -> props.setProperty("dataSource."+p, q==null?"":q.toString()));
            ((Map<String, ?>)configs).forEach((p, q) -> props.setProperty(p, q==null?"":q.toString()));
            String password = props.getProperty("password");
            if(StringUtil.isBlank(password)) {
            	password = RedisConfig.get("ds.password");
            	if(StringUtil.isBlank(password)) {
            		props.remove("password");
            	}else {
            		props.setProperty("password", password);
            	}
            }
            HikariConfig config = new HikariConfig(props);
            HikariDataSource ds = new HikariDataSource(config);
            DATASOURCEMAP.put(k, ds);
		});
		DATASOURCE = DATASOURCEMAP.get("mysql");
		QUERYRUNNER = new QueryRunner(DATASOURCE);
		log.info("mysql config loaded");
		
		TaskUtil.addShutdownHook(new Runnable() {
			@Override
			public void run() {
				log.info("mysql config shutdown");
				DATASOURCE.close();
			}
		});
	}
	
}
