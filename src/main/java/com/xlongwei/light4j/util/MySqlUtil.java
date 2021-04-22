package com.xlongwei.light4j.util;

import javax.sql.DataSource;

//import org.apache.commons.dbutils.QueryRunner;
//import org.apache.commons.dbutils.ResultSetHandler;
import org.beetl.sql.clazz.kit.StringKit;
import org.beetl.sql.core.ConnectionSourceHelper;
import org.beetl.sql.core.DefaultNameConversion;
import org.beetl.sql.core.Interceptor;
import org.beetl.sql.core.SQLManager;
import org.beetl.sql.core.db.MySqlStyle;
import org.beetl.sql.ext.DebugInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.networknt.service.SingletonServiceFactory;
import com.zaxxer.hikari.HikariDataSource;

/**
 * mysql util
 * <li>apijson使用Java风格命名表字段，因此beetlsql也使用默认的DefaultNameConversion
 * @author xlongwei
 */
public class MySqlUtil {
	public static final HikariDataSource DATASOURCE = (HikariDataSource)SingletonServiceFactory.getBean(DataSource.class);
//	public static final QueryRunner QUERYRUNNER = new QueryRunner(DATASOURCE);
	public static final SQLManager SQLMANAGER = SQLManager.newBuilder(ConnectionSourceHelper.getSingle(DATASOURCE)).setNc(new DefaultNameConversion() {
		@Override
		public  String getClassName(String tableName){
			return StringKit.toUpperCaseFirstOne(tableName); //类名使用表名+首字母大写，字段名与列名相同
		}
	}).setDbStyle(new MySqlStyle()).setSqlLoader("beetl/sql", "UTF-8").build();
	public static final Interceptor[] INTERS = new Interceptor[]{new DebugInterceptor()}, EMPTY_INTERS = new Interceptor[] {};
	private static final Logger log = LoggerFactory.getLogger(MySqlUtil.class);
	
	static {
		if(!SQLMANAGER.isProductMode()) {
			SQLMANAGER.setInters(INTERS);
		}
		TaskUtil.addShutdownHook(DATASOURCE);
		log.info("datasource config loaded");
	}
	
	/** QueryRunner回调 */
//	@FunctionalInterface
//	public interface QueryRunnerCallback<T> {
//		/**
//		 * 操作QueryRunner，必要时返回值
//		 * @param qr
//		 * @return
//		 * @throws Exception
//		 */
//		T doInQueryRunner(QueryRunner qr) throws Exception;
//		
//		public static class Query<T> implements QueryRunnerCallback<T>  {
//			private String sql;
//			private ResultSetHandler<T> rsh;
//			private Object[] params;
//			public Query(String sql, ResultSetHandler<T> rsh, Object... params) {
//				this.sql = sql;
//				this.rsh = rsh;
//				this.params = params;
//			}
//			@Override
//			public T doInQueryRunner(QueryRunner qr) throws Exception {
//				return qr.query(sql, rsh, params);
//			}
//		}
//		
//		public static class Insert<T> implements QueryRunnerCallback<T>  {
//			private String sql;
//			private ResultSetHandler<T> rsh;
//			private Object[] params;
//			public Insert(String sql, ResultSetHandler<T> rsh, Object... params) {
//				this.sql = sql;
//				this.rsh = rsh;
//				this.params = params;
//			}
//			@Override
//			public T doInQueryRunner(QueryRunner qr) throws Exception {
//				boolean isBatch = params!=null && params.length>0 && params[0].getClass().isArray();
//				if(isBatch) {
//					int length = params.length;
//					Object[][] batchParams = new Object[length][];
//					for(int i=0; i<length; i++) {
//						batchParams[i] = (Object[])params[i];
//					}
//					return qr.insertBatch(sql, rsh, batchParams);
//				}else {
//					return qr.insert(sql, rsh, params);
//				}
//			}
//		}
//		
//		public static class Update implements QueryRunnerCallback<Integer>  {
//			private String sql;
//			private Object[] params;
//			public Update(String sql, Object... params) {
//				this.sql = sql;
//				this.params = params;
//			}
//			@Override
//			public Integer doInQueryRunner(QueryRunner qr) throws Exception {
//				return qr.update(sql, params);
//			}
//		}
//	}
//	
//	public static <T> T execute(QueryRunnerCallback<T> callback) {
//		try {
//			return callback.doInQueryRunner(QUERYRUNNER);
//		}catch(Exception e) {
//			log.warn("mysql query fail: {} {}", e.getClass().getSimpleName(), e.getMessage());
//			return null;
//		}
//	}
	
	/** 基于MySql的序列号工具 */
	public static class Sequence {
		/** 获取name序列的下一个序列号 */
		public static long next(String name) {
			return next(name, 1L);
		}
		/** 获取name序列的下一个序列号 */
		public static long next(String name, long step) {
			try{
				return SQLMANAGER.execute("select sequence(#{name},#{step})", Long.class, StringUtil.params("name",name,"step",String.valueOf(step))).get(0);
			}catch(Exception e) {
				//新序列会异常
			}
			SQLMANAGER.executeUpdate("insert ignore sequence values(#{name},#{value})", StringUtil.params("name",name,"value","0"));
			return next(name, step);
		}
		/** 更新name序列，value为0时重置序列，为负时删除序列 */
		public static boolean update(String name, long value) {
			if(value < 0) {
				return 1 == SQLMANAGER.executeUpdate("delete from sequence where name=#{name}", StringUtil.params("name",name,"value","0"));
			}else {
				return 1 == SQLMANAGER.executeUpdate("update sequence set value=#{value} where name=#{name}", StringUtil.params("name",name,"value", String.valueOf(value)));
			}
		}
	}
}
