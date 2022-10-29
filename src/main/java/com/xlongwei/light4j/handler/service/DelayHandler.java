package com.xlongwei.light4j.handler.service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.DateUtil;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.HttpUtil;
import com.xlongwei.light4j.util.NumberUtil;
import com.xlongwei.light4j.util.RedisConfig;
import com.xlongwei.light4j.util.RedisUtil;
import com.xlongwei.light4j.util.StringUtil;
import com.xlongwei.light4j.util.TaskUtil;

import io.undertow.server.HttpServerExchange;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DelayHandler extends AbstractHandler {
    private byte[] byteKey = RedisUtil.byteKey(RedisConfig.CACHE, "delay");

    public DelayHandler() {
        TaskUtil.submitKeepRunning(() -> {
            while (true) {
                RedisConfig.execute((jedis) -> {
                    Set<byte[]> members = jedis.zrangeByScore(byteKey, 0, System.currentTimeMillis());
                    for (byte[] member : members) {
                        long zrem = jedis.zrem(byteKey, member);
                        if (zrem >= 1) {
                            String url = RedisUtil.stringValue(member);
                            log.info("pop {}", url);
                            TaskUtil.submit(() -> {
                                HttpUtil.get(url, null);
                            });
                        }
                    }
                    return null;
                });
                TaskUtil.sleep(TimeUnit.SECONDS.toMillis(1));
            }
        });
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        String url = HandlerUtil.getParamOrBody(exchange, "url");
        long delay = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "delay"), 0);
        TimeUnit unit = TimeUnit.valueOf(
                StringUtils.defaultString(HandlerUtil.getParam(exchange, "unit"), TimeUnit.MINUTES.name()));
        if (StringUtil.isUrl(url) && delay > 0) {
            final long score = unit.toMillis(delay) + System.currentTimeMillis();
            byte[] byteValue = RedisUtil.byteValue(url);
            String until = DateUtil.datetimeFormat.format(score);
            log.info("push {} until {}", url, until);
            RedisConfig.execute((jedis) -> {
                jedis.zadd(byteKey, score, byteValue);
                return null;
            });
            HandlerUtil.setResp(exchange, StringUtil.params("until", until));
        }
    }

}
