package com.xlongwei.light4j.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.beetl.sql.core.ClasspathLoader;
import org.beetl.sql.core.ConnectionSourceHelper;
import org.beetl.sql.core.Interceptor;
import org.beetl.sql.core.SQLManager;
import org.beetl.sql.core.UnderlinedNameConversion;
import org.beetl.sql.core.db.MySqlStyle;
import org.beetl.sql.ext.DebugInterceptor;

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
	public static final SQLManager SQLMANAGER;
	
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
		SQLMANAGER = new SQLManager(new MySqlStyle(),new ClasspathLoader("/beetl/sql"),ConnectionSourceHelper.getSingle(DATASOURCE),new  UnderlinedNameConversion(),new Interceptor[]{new DebugInterceptor()});
		log.info("mysql config loaded");
		
		TaskUtil.addShutdownHook((Runnable)() -> {
				log.info("mysql config shutdown");
				DATASOURCE.close();
		});
	}
	
	/** QueryRunner回调 */
	@FunctionalInterface
	public interface QueryRunnerCallback<T> {
		/**
		 * 操作QueryRunner，必要时返回值
		 * @param qr
		 * @return
		 * @throws Exception
		 */
		T doInQueryRunner(QueryRunner qr) throws Exception;
		
		public static class Query<T> implements QueryRunnerCallback<T>  {
			private String sql;
			private ResultSetHandler<T> rsh;
			private Object[] params;
			public Query(String sql, ResultSetHandler<T> rsh, Object... params) {
				this.sql = sql;
				this.rsh = rsh;
				this.params = params;
			}
			@Override
			public T doInQueryRunner(QueryRunner qr) throws Exception {
				return qr.query(sql, rsh, params);
			}
		}
		
		public static class Insert<T> implements QueryRunnerCallback<T>  {
			private String sql;
			private ResultSetHandler<T> rsh;
			private Object[] params;
			public Insert(String sql, ResultSetHandler<T> rsh, Object... params) {
				this.sql = sql;
				this.rsh = rsh;
				this.params = params;
			}
			@Override
			public T doInQueryRunner(QueryRunner qr) throws Exception {
				boolean isBatch = params!=null && params.length>0 && params[0].getClass().isArray();
				if(isBatch) {
					int length = params.length;
					Object[][] batchParams = new Object[length][];
					for(int i=0; i<length; i++) {
						batchParams[i] = (Object[])params[i];
					}
					return qr.insertBatch(sql, rsh, batchParams);
				}else {
					return qr.insert(sql, rsh, params);
				}
			}
		}
		
		public static class Update implements QueryRunnerCallback<Integer>  {
			private String sql;
			private Object[] params;
			public Update(String sql, Object... params) {
				this.sql = sql;
				this.params = params;
			}
			@Override
			public Integer doInQueryRunner(QueryRunner qr) throws Exception {
				return qr.update(sql, params);
			}
		}
	}
	
	public static <T> T execute(QueryRunnerCallback<T> callback) {
		try {
			return callback.doInQueryRunner(QUERYRUNNER);
		}catch(Exception e) {
			log.warn("mysql query fail: {}", e.getMessage());
			return null;
		}
	}
}
