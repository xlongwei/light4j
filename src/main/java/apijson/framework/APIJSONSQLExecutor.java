/*Copyright ©2016 TommyLemon(https://github.com/TommyLemon/APIJSON)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/

package apijson.framework;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.postgresql.util.PGobject;

import com.alibaba.fastjson.JSONObject;
import com.xlongwei.light4j.util.MySqlUtil;

import apijson.JSON;
import apijson.Log;
import apijson.NotNull;
import apijson.orm.AbstractSQLExecutor;
import apijson.orm.SQLConfig;
import lombok.extern.slf4j.Slf4j;


/**executor for query(read) or update(write) MySQL database
 * @author Lemon
 */
@Slf4j
public class APIJSONSQLExecutor extends AbstractSQLExecutor {
	public static final String TAG = "APIJSONSQLExecutor";

//	static {
//		try { //加载驱动程序
//			Log.d(TAG, "尝试加载 MySQL 8 驱动 <<<<<<<<<<<<<<<<<<<<< ");
//			Class.forName("com.mysql.cj.jdbc.Driver");
//			Log.d(TAG, "成功加载 MySQL 8 驱动！>>>>>>>>>>>>>>>>>>>>>");
//		}
//		catch (ClassNotFoundException e) {
//			Log.e(TAG, "加载 MySQL 8 驱动失败，请检查 pom.xml 中 mysql-connector-java 版本是否存在以及可用 ！！！");
//			e.printStackTrace();
//
//			try { //加载驱动程序
//				Log.d(TAG, "尝试加载 MySQL 7 及以下版本的 驱动 <<<<<<<<<<<<<<<<<<<<< ");
//				Class.forName("com.mysql.jdbc.Driver");
//				Log.d(TAG, "成功加载 MySQL 7 及以下版本的 驱动！>>>>>>>>>>>>>>>>>>>>> ");
//			}
//			catch (ClassNotFoundException e2) {
//				Log.e(TAG, "加载 MySQL 7 及以下版本的 驱动失败，请检查 pom.xml 中 mysql-connector-java 版本是否存在以及可用 ！！！");
//				e2.printStackTrace();
//			}
//		}
//
//		try { //加载驱动程序
//			Log.d(TAG, "尝试加载 PostgresSQL 驱动 <<<<<<<<<<<<<<<<<<<<< ");
//			Class.forName("org.postgresql.Driver");
//			Log.d(TAG, "成功加载 PostgresSQL 驱动！>>>>>>>>>>>>>>>>>>>>> ");
//		}
//		catch (ClassNotFoundException e) {
//			e.printStackTrace();
//			Log.e(TAG, "加载 PostgresSQL 驱动失败，请检查 libs 目录中 postgresql.jar 版本是否存在以及可用 ！！！");
//		}
//		
//	}


	@Override
	public PreparedStatement setArgument(@NotNull SQLConfig config, @NotNull PreparedStatement statement, int index, Object value) throws SQLException {
		if (config.isPostgreSQL() && JSON.isBooleanOrNumberOrString(value) == false) {
			PGobject o = new PGobject();
			o.setType("jsonb");
			o.setValue(value == null ? null : value.toString());
			statement.setObject(index + 1, o); //PostgreSQL 除了基本类型，其它的必须通过 PGobject 设置进去，否则 jsonb = varchar 等报错
			return statement;
		}
		
		return super.setArgument(config, statement, index, value);
	}


	@Override
	protected Object getValue(SQLConfig config, ResultSet rs, ResultSetMetaData rsmd, int tablePosition,
			JSONObject table, int columnIndex, String lable, Map<String, JSONObject> childMap) throws Exception {
		
		Object value = super.getValue(config, rs, rsmd, tablePosition, table, columnIndex, lable, childMap);

		return value instanceof PGobject ? JSON.parse(((PGobject) value).getValue()) : value;
	}

	/** 用于判断哪个请求会有连接未关闭 */
	public static final AtomicInteger borrowedConnections = new AtomicInteger(0);
	/**
	 * apijson基本上是单线程的，因此可以使用ThreadLocal，如果有较多多线程，那么就更难跟踪已打开的数据库连接了
	 */
	public static final ThreadLocal<Connection> threadConnection = ThreadLocal.withInitial(() -> {
		try{
			Connection conn = MySqlUtil.DATASOURCE.getConnection();
			Log.v(TAG, "connection borrowed to {}"+borrowedConnections.incrementAndGet());
			return conn;
		}catch(Exception e) {
			log.info("{} {}", e.getClass().getSimpleName(), e.getMessage());
			return null;
		}
	});
	/**
	 * 从连接池获取连接，不需要单独加载驱动类
	 */
	@Override
	public synchronized Connection getConnection(SQLConfig config) throws Exception {
		//super.close()会设置cacheMap、connectionMap为null，而此方法有可能在close之后再次被调用，因此重新赋值避免空指针
		if(cacheMap == null) {
			cacheMap = new HashMap<>();
		}
		if(connectionMap == null) {
			connectionMap = new HashMap<>();
		}
		Connection connection = connectionMap.get(config.getDatabase());
		if(connection == null) {
			connection = threadConnection.get();
			connectionMap.put(config.getDatabase(), connection);
		}
		//AbstractSQLExecutor每次通过DriverManager获取连接，这里提前从连接池获取连接，close方法会释放连接（回到连接池）
		return super.getConnection(config);
	}


	@Override
	public synchronized void close() {
		if(connectionMap!=null && !connectionMap.isEmpty()) {
			Set<Connection> conns = new HashSet<>(connectionMap.values());
			Connection conn = threadConnection.get();
			conns.add(conn);
			try {
				if(conns.size() > 1) {
					log.error("apijson connection leak, conns={}", conns.size());
				}
				for(Connection con : conns) {
					if(!con.isClosed()) {
						con.close();
						borrowedConnections.decrementAndGet();
					}
				}
				if(conns.size() > 1) {
					log.info("apijson connection return to {}", conns.size());
				}else {
					Log.v(TAG, "connection returned to {}"+borrowedConnections.get());
				}
			}catch(Exception e) {
				log.info("{} {}", e.getClass().getSimpleName(), e.getMessage());
			}
			threadConnection.remove();
			connectionMap.clear();
		}
		if(cacheMap != null) {
			super.close();
		}
	}

}
