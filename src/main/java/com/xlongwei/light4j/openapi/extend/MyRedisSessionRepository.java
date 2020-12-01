package com.xlongwei.light4j.openapi.extend;

import org.redisson.config.Config;

import com.alibaba.fastjson.JSONObject;
import com.networknt.session.redis.RedisSessionRepository;
import com.xlongwei.light4j.util.FileUtil;
import com.xlongwei.light4j.util.FileUtil.CharsetNames;
import com.xlongwei.light4j.util.JsonUtil;
import com.xlongwei.light4j.util.RedisConfig;

import lombok.extern.slf4j.Slf4j;

/**
 * service.yml可选配置RedisSessionRepository，默认使用此类会使用-Dredis.configDb的配置
 * @author xlongwei
 */
@Slf4j
public class MyRedisSessionRepository extends RedisSessionRepository {
	static Config configRef = null;
	static {
		String address = "redis://" + RedisConfig.host + ":" + RedisConfig.port;
		String configJsonFile = "/singleNodeConfig.json";
		try {
			String configJsonString = FileUtil.readString(MyRedisSessionRepository.class.getResourceAsStream(configJsonFile), CharsetNames.UTF_8);
			JSONObject configJson = JsonUtil.parse(configJsonString);
			JSONObject singleServerConfig = configJson.getJSONObject("singleServerConfig");
//			if(!singleServerConfig.containsKey("address")) {
				singleServerConfig.put("address", address);
				configJsonString = configJson.toJSONString();
//			}
			log.info("singleServerConfig.address={}", singleServerConfig.getString("address"));
			config = Config.fromJSON(configJsonString);
		}catch(Exception e) {
			config = new Config();
			config.useSingleServer().setAddress(address);
		}
		configRef = config;
	}
	public MyRedisSessionRepository() {
		super();
		assert(configRef == config);
	}
}
