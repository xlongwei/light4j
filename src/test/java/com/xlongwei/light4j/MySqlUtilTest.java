package com.xlongwei.light4j;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.beetl.sql.core.SQLManager;
import org.beetl.sql.core.query.LambdaQuery;
import org.junit.Test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.xlongwei.light4j.beetl.dao.UserDao;
import com.xlongwei.light4j.beetl.model.User;
import com.xlongwei.light4j.handler.DemoHandler;
import com.xlongwei.light4j.util.MySqlUtil;
import com.xlongwei.light4j.util.StringUtil;

import cn.hutool.extra.template.Template;
import lombok.extern.slf4j.Slf4j;

/**
 * MySqlUtilTest
 * @author xlongwei
 *
 */
@Slf4j
public class MySqlUtilTest {
	
	@Test public void dataSource() throws Exception {
		String jdbcUrl = MySqlUtil.DATASOURCE.getJdbcUrl();
		String username = MySqlUtil.DATASOURCE.getUsername();
		String password = MySqlUtil.DATASOURCE.getPassword();
		String catalog = MySqlUtil.DATASOURCE.getCatalog();
		String schema = MySqlUtil.DATASOURCE.getSchema();
		log.info("jdbcUrl={} username={} password={} catalog={} schema={}", jdbcUrl, username, password, catalog, schema);
		
		Connection conn = MySqlUtil.DATASOURCE.getConnection();
		catalog = conn.getCatalog();
		schema = conn.getSchema();
		int ti = conn.getTransactionIsolation();
		int nt = conn.getNetworkTimeout();
		log.info("conn catalog={} schema={} TransactionIsolation={} NetworkTimeout={}", catalog, schema, ti, nt);
		
		DatabaseMetaData metaData = conn.getMetaData();
		String url = metaData.getURL();
		String dbVersion = metaData.getDatabaseProductVersion();
		String driverVersion = metaData.getDriverVersion();
		log.info("url={} dbVersion={} driverVersion={}", url, dbVersion, driverVersion);
		conn.close();
	}

	@Test public void beetlGen() throws Exception {
		SQLManager sqlManager = MySqlUtil.SQLMANAGER;
//		sqlManager.genPojoCodeToConsole("user");
//	    sqlManager.genSQLTemplateToConsole("user");
//	    sqlManager.genBuiltInSqlToConsole(User.class);
	}
	
	@Test public void beetlQuery() {
		SQLManager sqlManager = MySqlUtil.SQLMANAGER;
		//简单查询：unique异常、single空、all全部+分页、allCout计数
		User user = sqlManager.single(User.class, 1L);
		log.info("user={}", user);
		//模板查询、更新
		user = new User();
		user.setId(1);
		List<User> list = sqlManager.template(user);
		log.info("template list={}", list);
		//Query查询
		LambdaQuery<User> query = sqlManager.lambdaQuery(User.class);
		list = query.andEq("id", "1").select();
		log.info("query list={}", list);
		//user.md查询
//		list = sqlManager.select("user.sample", User.class);
//		log.info("sample list={}", list);
		//dao查询
		UserDao dao = sqlManager.getMapper(UserDao.class);
		user.setName("admin");
		list = dao.sample(user);
		log.info("dao list={}", list);
	}
	
//	@Test
	public void beetlUpdate() {
		UserDao dao = MySqlUtil.SQLMANAGER.getMapper(UserDao.class);
		User user = new User();
		user.setName("admin");
		dao.insert(user);
		log.info("user={}", user);
	}
	
	@Test public void beetlDemo() {
		Template template = DemoHandler.engine.getTemplate("index.html");
		Map<String, String> map = StringUtil.params("ip", "127.0.0.1", "region", "localhost");
		String html = template.render(map);
		log.info("html={}", html);
	}

	/**
	 * apijson默认MySQL5.7.22支持json类型，老版本MySQL改为了varchar(65535)，因此在apijson.JSON.getCorrectJson方法手动将String="[1,2,3]"转换为JSONArray=[1,2,3]，以便fastjson正确转换List类型
	 */
	@Test public void apijson() {
		String json = "{\"user\":{\"id\":38710,\"sex\":0,\"name\":\"TommyLemon\",\"tag\":\"Android&Java\",\"head\":\"http://static.oschina.net/uploads/user/1218/2437072_100.jpg?t=1461076033000\",\"contactIdList\":\"[82003, 82005, 90814, 82004, 82009, 82002, 82044, 93793, 70793]\",\"pictureList\":\"[\\\"http://static.oschina.net/uploads/user/1218/2437072_100.jpg?t=1461076033000\\\", \\\"http://common.cnblogs.com/images/icon_weibo_24.png\\\"]\",\"date\":\"2017-02-01 19:21:50.0\"},\"ok\":true,\"code\":200,\"msg\":\"success\"}";
		JSONObject obj = JSON.parseObject(json).getJSONObject("user");
		String json1 = handle(obj).toJSONString();
		String json2 = apijson.JSON.getCorrectJson(obj.toString());
		log.info("\njson1={}\njson2={}\nequals={}", json1,json2,json1.equals(json2));
		json = json2;
		int features = com.alibaba.fastjson.JSON.DEFAULT_PARSER_FEATURE;
		features |= Feature.OrderedField.getMask();
		features |= Feature.SupportArrayToBean.getMask();
		Object user = JSON.parseObject(json, com.xlongwei.light4j.apijson.model.User.class, features);
		log.info("user={}", user);
		log.info("user={}",apijson.JSON.parseObject(json, com.xlongwei.light4j.apijson.model.User.class));
	}

	private JSON handle(JSONObject json) {
		for(Entry<String, Object> entry : json.entrySet()) {
			Object value = entry.getValue();
			if(value instanceof String) {
				String str = (String)value;
				if(str.length()>=2 && str.charAt(0)=='[' && str.charAt(str.length()-1)==']') {
					entry.setValue(JSON.parseArray(str));
				}
			}else if(value instanceof JSONObject) {
				handle((JSONObject)value);
			}
		}
		return json;
	}
}
