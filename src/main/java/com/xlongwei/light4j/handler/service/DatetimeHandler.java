package com.xlongwei.light4j.handler.service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.time.FastDateFormat;

import com.alibaba.fastjson.JSONObject;
import com.networknt.utility.StringUtils;
import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.DateUtil;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.HolidayUtil;
import com.xlongwei.light4j.util.IdWorker.SystemClock;
import com.xlongwei.light4j.util.JsonUtil;
import com.xlongwei.light4j.util.NumberUtil;
import com.xlongwei.light4j.util.RedisConfig;
import com.xlongwei.light4j.util.StringUtil;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

/**
 * datetime handler
 * @author xlongwei
 *
 */
public class DatetimeHandler extends AbstractHandler {
	private static FastDateFormat fastDateFormat = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");
	
	public DatetimeHandler() {
		String holidays = RedisConfig.get("datetime.holidays");
		if(StringUtil.isBlank(holidays)) {
			return;
		}else {
			JSONObject json = JsonUtil.parseNew(holidays);
			for(String key : json.keySet()) {
				HolidayUtil.addPlan(key, json.getString(key));
			}
		}
	}

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		String path = exchange.getAttachment(AbstractHandler.PATH);
		if(StringUtils.isBlank(path)) {
			exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
			HandlerUtil.setCorsHeaders(exchange);
	        exchange.setStatusCode(200);
	        String datetime = fastDateFormat.format(SystemClock.now());
			exchange.getResponseSender().send("{\"datetime\":\""+datetime+"\"}");
			return;
		}
		Date day = DateUtil.parseNow(HandlerUtil.getParam(exchange, "day"));
		Map<String, Object> map = new HashMap<>(4);
		switch(path) {
		case "isworkday":
			map.put("isworkday", HolidayUtil.isworkday(day));
			break;
		case "isholiday":
			map.put("isholiday", HolidayUtil.isholiday(day));
			break;
		case "isweekend":
			map.put("isweekend", HolidayUtil.isweekend(day));
			break;
		case "nextworkday":
			day = HolidayUtil.nextworkday(day, NumberUtil.parseBoolean(HandlerUtil.getParam(exchange, "skipweekend"), false));
			map.put("nextworkday", DateUtil.dateFormat.format(day));
			break;
		case "offsetworkday":
			day = HolidayUtil.offsetworkday(day, NumberUtil.parseInt(HandlerUtil.getParam(exchange, "offset"), 0), NumberUtil.parseBoolean(HandlerUtil.getParam(exchange, "skipweekend"), false));
			map.put("offsetworkday", DateUtil.dateFormat.format(day));
			break;
		case "offsetday":
			day = cn.hutool.core.date.DateUtil.offsetDay(day, NumberUtil.parseInt(HandlerUtil.getParam(exchange, "offset"), 0));
			map.put("offsetday", DateUtil.dateFormat.format(day));
			break;
		case "holiday":
			String name = HandlerUtil.getParam(exchange, "name");
			String plan = StringUtil.isBlank(name) ? null : HolidayUtil.plans.get(DateUtil.yearFormat.format(day)+"."+name);
			if(plan==null) {
				Integer flag = HolidayUtil.holidays.get(HolidayUtil.dateFormat.format(day));
				if(flag!=null && flag.intValue()>0) {
					name = HolidayUtil.nameOf(flag);
					plan = StringUtil.isBlank(name) ? null : HolidayUtil.plans.get(DateUtil.yearFormat.format(day)+"."+name);
				}
			}
			if(StringUtil.hasLength(plan)) {
				map.put("holiday", name);
				map.put("remark", plan);
			}
			break;
		case "age":
			map.put("age", cn.hutool.core.date.DateUtil.age(DateUtil.parseNow(HandlerUtil.getParam(exchange, "birth")), day));
			break;
		case "betweenday":
		case "betweenworkday":
			Date start = DateUtil.parse(HandlerUtil.getParam(exchange, "start")), end = DateUtil.parse(HandlerUtil.getParam(exchange, "end"));
			if(start!=null || end!=null) {
				start = start==null ? day : start;
				end = end==null ? day : end;
				long between = "betweenday".equals(path) ? cn.hutool.core.date.DateUtil.betweenDay(start, end, true)
						: HolidayUtil.betweenworkday(start, end, NumberUtil.parseBoolean(HandlerUtil.getParam(exchange, "skipweekend"), false));
				map.put(path, between);
			}
			break;
		default:
			break;
		}
		HandlerUtil.setResp(exchange, map);
	}

}
