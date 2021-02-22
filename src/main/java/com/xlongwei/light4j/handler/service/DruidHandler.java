package com.xlongwei.light4j.handler.service;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.DruidUtil;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.NumberUtil;
import com.xlongwei.light4j.util.StringUtil;

import io.undertow.server.HttpServerExchange;

/**
 * 封装druid密码处理工具
 * @author xlongwei
 *
 */
public class DruidHandler extends AbstractHandler {

	/**
	 * <pre>
	 * publicKey有默认值，可以不提供
	 * spring.datasource.publicKey=MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAKHGwq7q2RmwuRgKxBypQHw0mYu4BQZ3eMsTrdK8E6igRcxsobUC7uT0SoxIjl1WveWniCASejoQtn/BY6hVKWsCAwEAAQ==
	 */
	public void genKeyPair(HttpServerExchange exchange) throws Exception {
		String password = HandlerUtil.getParam(exchange, "password");
		int keySize = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "keySize"), 512);
		String[] genKeyPair = DruidUtil.genKeyPair(keySize);
		String privateKey = genKeyPair[0];
		String publicKey = genKeyPair[1];
		Map<String, String> resp = StringUtil.params("privateKey", privateKey, "publicKey", publicKey);
		if(password != null) {
			resp.put("password", DruidUtil.encrypt(privateKey, password));
		}
		HandlerUtil.setResp(exchange, resp);
	}
	
	/**
	 * privateKey有默认值，可以不提供
	 * privateKey=MIIBVAIBADANBgkqhkiG9w0BAQEFAASCAT4wggE6AgEAAkEAocbCrurZGbC5GArEHKlAfDSZi7gFBnd4yxOt0rwTqKBFzGyhtQLu5PRKjEiOXVa95aeIIBJ6OhC2f8FjqFUpawIDAQABAkAPejKaBYHrwUqUEEOe8lpnB6lBAsQIUFnQI/vXU4MV+MhIzW0BLVZCiarIQqUXeOhThVWXKFt8GxCykrrUsQ6BAiEA4vMVxEHBovz1di3aozzFvSMdsjTcYRRo82hS5Ru2/OECIQC2fAPoXixVTVY7bNMeuxCP4954ZkXp7fEPDINCjcQDywIgcc8XLkkPcs3Jxk7uYofaXaPbg39wuJpEmzPIxi3k0OECIGubmdpOnin3HuCP/bbjbJLNNoUdGiEmFL5hDI4UdwAdAiEAtcAwbm08bKN7pwwvyqaCBC//VnEWaq39DCzxr+Z2EIk=
	 */
	public void encrypt(HttpServerExchange exchange) throws Exception {
		String password = StringUtils.trimToEmpty(HandlerUtil.getParam(exchange, "password"));
		String privateKey = StringUtils.trimToNull(HandlerUtil.getParam(exchange, "privateKey"));
		String encrypt = DruidUtil.encrypt(privateKey, password);
		HandlerUtil.setResp(exchange, StringUtil.params("encrypt", encrypt));
	}
	
	/**
	 * <pre>
	 * spring.datasource.filters=config,stat,wall,log4j
	 * spring.datasource.connectionProperties=config.decrypt=true;druid.stat.mergeSql=true;druid.stat.slowSqlMillis=5000
	 * spring.datasource.password=brW4Fq8Z9eiWpM1+0zcvgrkqX+b/FQq0P71dDxF3FxoqX7KINU2JTTqgru0UOr3Pp9rsPFqTbZvBUwnIbjR9jA==
	 */
	public void decrypt(HttpServerExchange exchange) throws Exception {
		String password = StringUtils.trimToEmpty(HandlerUtil.getParam(exchange, "password"));
		String publicKey = StringUtils.trimToNull(HandlerUtil.getParam(exchange, "publicKey"));
		String decrypt = DruidUtil.decrypt(publicKey, password);
		HandlerUtil.setResp(exchange, StringUtil.params("decrypt", decrypt));
	}

}
