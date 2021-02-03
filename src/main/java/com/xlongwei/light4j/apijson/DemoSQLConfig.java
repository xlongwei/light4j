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

package com.xlongwei.light4j.apijson;

import static apijson.framework.APIJSONConstant.ID;
import static apijson.framework.APIJSONConstant.PRIVACY_;
import static apijson.framework.APIJSONConstant.USER_;
import static apijson.framework.APIJSONConstant.USER_ID;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

import com.xlongwei.light4j.util.MySqlUtil;

import apijson.Log;
import apijson.RequestMethod;
import apijson.framework.APIJSONSQLConfig;
import apijson.orm.AbstractSQLConfig;


/**SQL配置
 * TiDB 用法和 MySQL 一致
 * @author Lemon
 */
public class DemoSQLConfig extends APIJSONSQLConfig {

	public DemoSQLConfig() {
		super();
	}
	public DemoSQLConfig(RequestMethod method, String table) {
		super(method, table);
	}
	
	private static String url, catalog, username, password, version;
	static void getDBInfo() {
		try(Connection conn = MySqlUtil.DATASOURCE.getConnection()) {
			String jdbcUrl = MySqlUtil.DATASOURCE.getJdbcUrl();
			int pos = jdbcUrl.indexOf('?');
			//AbstractSQLExecutor.getConnection(SQLConfig)会自动补充参数
			url = pos==-1 ? jdbcUrl : jdbcUrl.substring(0, pos);
			catalog = conn.getCatalog();
			username = MySqlUtil.DATASOURCE.getUsername();
			password = MySqlUtil.DATASOURCE.getPassword();
			DatabaseMetaData meta = conn.getMetaData();
			version = meta.getDatabaseProductVersion();
		}catch(Exception e) {
			Log.e(TAG, "获取数据库信息失败："+e.getClass().getSimpleName()+" "+e.getMessage());
		}
	}

	static {
		getDBInfo();
		DEFAULT_DATABASE = DATABASE_MYSQL;  //默认数据库类型，改成你自己的
		DEFAULT_SCHEMA = catalog!=null ? catalog : "sys";  //默认模式名，改成你自己的，默认情况是 MySQL: sys, PostgreSQL: public, SQL Server: dbo, Oracle: 

		//  由 DemoVerifier.init 方法读取数据库 Access 表来替代手动输入配置
		//		//表名映射，隐藏真实表名，对安全要求很高的表可以这么做
		//		TABLE_KEY_MAP.put(User.class.getSimpleName(), "apijson_user");
		//		TABLE_KEY_MAP.put(Privacy.class.getSimpleName(), "apijson_privacy");

		//主键名映射
		SIMPLE_CALLBACK = new SimpleCallback() {

			@Override
			public AbstractSQLConfig getSQLConfig(RequestMethod method, String database, String schema, String table) {
				return new DemoSQLConfig(method, table);
			}

			//取消注释来实现自定义各个表的主键名
			//			@Override
			//			public String getIdKey(String database, String schema, String table) {
			//				return StringUtil.firstCase(table + "Id");  // userId, comemntId ...
			//				//		return StringUtil.toLowerCase(t) + "_id";  // user_id, comemnt_id ...
			//				//		return StringUtil.toUpperCase(t) + "_ID";  // USER_ID, COMMENT_ID ...
			//			}

			@Override
			public String getUserIdKey(String database, String schema, String table) {
				return USER_.equals(table) || PRIVACY_.equals(table) ? ID : USER_ID; // id / userId
			}

			//取消注释来实现数据库自增 id
			//			@Override
			//			public Object newId(RequestMethod method, String database, String schema, String table) {
			//				return null; // return null 则不生成 id，一般用于数据库自增 id
			//			}

			//			@Override
			//			public void onMissingKey4Combine(String name, JSONObject request, String combine, String item, String key) throws Exception {
			////				super.onMissingKey4Combine(name, request, combine, item, key);
			//			}
		};

		// 自定义原始 SQL 片段，其它功能满足不了时才用它，只有 RAW_MAP 配置了的 key 才允许前端传
		RAW_MAP.put("`to`.`id`", "");  // 空字符串 "" 表示用 key 的值 `to`.`id`
		RAW_MAP.put("to.momentId", "`to`.`momentId`");  // 最终以 `to`.`userId` 拼接 SQL，相比以上写法可以让前端写起来更简单
		RAW_MAP.put("(`Comment`.`userId`=`to`.`userId`)", "");  // 已经是一个条件表达式了，用 () 包裹是为了避免 JSON 中的 key 拼接在前面导致 SQL 出错
		RAW_MAP.put("sum(if(userId%2=0,1,0))", "");  // 超过单个函数的 SQL 表达式
		RAW_MAP.put("sumUserIdIsEven", "sum(if(`userId`%2=0,1,0)) AS sumUserIdIsEven");  // 简化前端传参
		RAW_MAP.put("SUBSTRING_INDEX(SUBSTRING_INDEX(content,',',1),',',-1)", "");  // APIAuto 不支持 '，可以用 Postman 测
		RAW_MAP.put("SUBSTRING_INDEX(SUBSTRING_INDEX(content,'.',1),'.',-1) AS subContent", "");  // APIAuto 不支持 '，可以用 Postman 测
		RAW_MAP.put("commentWhereItem1","(`Comment`.`userId` = 38710 AND `Comment`.`momentId` = 470)");
		RAW_MAP.put("to_days(now())-to_days(`date`)<=7","");  // 给 @having 使用
	}


	@Override
	public String getDBVersion() {
		if (isMySQL()) {
			return version!=null ? version : "5.7.22"; //"8.0.11"; //改成你自己的 MySQL 或 PostgreSQL 数据库版本号 //MYSQL 8 和 7 使用的 JDBC 配置不一样
		}
		if (isPostgreSQL()) {
			return "9.6.15"; //改成你自己的
		}
		if (isSQLServer()) {
			return "2016"; //改成你自己的
		}
		if (isOracle()) {
			return "18c"; //改成你自己的
		}
		if (isDb2()) {
			return "11.5"; //改成你自己的
		}
		return null;
	}
	@Override
	public String getDBUri() {
		if (isMySQL()) {
			return url!=null ? url : "jdbc:mysql://localhost:3306"; //改成你自己的，TiDB 可以当成 MySQL 使用，默认端口为 4000
		}
		if (isPostgreSQL()) {
			return "jdbc:postgresql://localhost:5432/postgres"; //改成你自己的
		}
		if (isSQLServer()) {
			return "jdbc:jtds:sqlserver://localhost:1433/pubs;instance=SQLEXPRESS"; //改成你自己的
		}
		if (isOracle()) {
			return "jdbc:oracle:thin:@localhost:1521:orcl"; //改成你自己的
		}
		if (isDb2()) {
			return "jdbc:db2://localhost:50000/BLUDB"; //改成你自己的
		}
		return null;
	}
	@Override
	public String getDBAccount() {
		if (isMySQL()) {
			return username!=null ? username : "root";  //改成你自己的
		}
		if (isPostgreSQL()) {
			return "postgres";  //改成你自己的
		}
		if (isSQLServer()) {
			return "sa";  //改成你自己的
		}
		if (isOracle()) {
			return "scott";  //改成你自己的
		}
		if (isDb2()) {
			return "db2admin"; //改成你自己的
		}
		return null;
	}
	@Override
	public String getDBPassword() {
		if (isMySQL()) {
			return password!=null ? password : "admin";  //改成你自己的，TiDB 可以当成 MySQL 使用， 默认密码为空字符串 ""
		}
		if (isPostgreSQL()) {
			return null;  //改成你自己的
		}
		if (isSQLServer()) {
			return "apijson@123";  //改成你自己的
		}
		if (isOracle()) {
			return "tiger";  //改成你自己的
		}
		if (isDb2()) {
			return "123"; //改成你自己的
		}
		return null;
	}
	
	//取消注释后，默认的数据库类型会由 MySQL 改为 PostgreSQL
	//	@Override
	//	public String getDatabase() {
	//		String db = super.getDatabase();
	//		return db == null ? DATABASE_POSTGRESQL : db;
	//	}

	//如果确定只用一种数据库，可以重写方法，这种数据库直接 return true，其它数据库直接 return false，来减少判断，提高性能
	//	@Override
	//	public boolean isMySQL() {
	//		return true;
	//	}
	//	@Override
	//	public boolean isPostgreSQL() {
	//		return false;
	//	}
	//	@Override
	//	public boolean isSQLServer() {
	//		return false;
	//	}
	//	@Override
	//	public boolean isOracle() {
	//		return false;
	//	}
	//	@Override
	//	public boolean isDb2() {
	//		return false;
	//	}

}
