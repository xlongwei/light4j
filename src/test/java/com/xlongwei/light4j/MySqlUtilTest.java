package com.xlongwei.light4j;

import java.util.List;
import java.util.Map;

import org.beetl.sql.core.SQLManager;
import org.beetl.sql.core.mapper.BaseMapper;
import org.beetl.sql.core.query.LambdaQuery;
import org.junit.Test;

import com.xlongwei.light4j.handler.DemoHandler;
import com.xlongwei.light4j.util.MySqlUtil;
import com.xlongwei.light4j.util.StringUtil;

import cn.hutool.extra.template.Template;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * MySqlUtilTest
 * @author xlongwei
 *
 */
@Slf4j
public class MySqlUtilTest {

	@Test public void beetlGen() throws Exception {
		SQLManager sqlManager = MySqlUtil.SQLMANAGER;
		sqlManager.genPojoCodeToConsole("user");
	    sqlManager.genSQLTemplateToConsole("user");
	    sqlManager.genBuiltInSqlToConsole(User.class);
	}
	
	@Test public void beetlQuery() {
		SQLManager sqlManager = MySqlUtil.SQLMANAGER;
		//简单查询：unique异常、single空、all全部+分页、allCout计数
		User user = sqlManager.single(User.class, 1L);
		log.info("user={}", user);
		//模板查询、更新
		user = new User();
		user.setId(1L);
		List<User> list = sqlManager.template(user);
		log.info("template list={}", list);
		//Query查询
		LambdaQuery<User> query = sqlManager.lambdaQuery(User.class);
		list = query.andEq("id", "1").select();
		log.info("query list={}", list);
		//user.md查询
		list = sqlManager.select("user.sample", User.class);
		log.info("sample list={}", list);
		//dao查询
		UserDao dao = sqlManager.getMapper(UserDao.class);
		user.setUserName("admin");
		list = dao.sample(user);
		log.info("dao list={}", list);
	}
	
	@Test public void beetlDemo() {
		Template template = DemoHandler.engine.getTemplate("index.html");
		Map<String, String> map = StringUtil.params("ip", "127.0.0.1", "region", "localhost");
		String html = template.render(map);
		log.info("html={}", html);
	}
	
	@Data
	public static class User {
		private Long id ;
		private String email ;
		private String userName ;
	}
	
	public interface UserDao extends BaseMapper<User> {
		/** sample */
		List<User> sample(User user);
	}
}
