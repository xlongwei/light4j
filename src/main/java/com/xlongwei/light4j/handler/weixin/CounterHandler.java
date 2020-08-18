package com.xlongwei.light4j.handler.weixin;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.DateUtils;

import com.xlongwei.light4j.util.AdtUtil.PairList;
import com.xlongwei.light4j.util.AdtUtil.PairList.PriorityPairList.PriorityComparator;
import com.xlongwei.light4j.handler.ServiceHandler;
import com.xlongwei.light4j.util.ConfigUtil;
import com.xlongwei.light4j.util.DateUtil;
import com.xlongwei.light4j.util.IdWorker.SystemClock;
import com.xlongwei.light4j.util.NumberUtil;
import com.xlongwei.light4j.util.RedisConfig;
import com.xlongwei.light4j.util.StringUtil;
import com.xlongwei.light4j.util.TaskUtil;
import com.xlongwei.light4j.util.WeixinUtil.AbstractMessageHandler.AbstractTextHandler;

/**
 * echo counter info
 * @author xlongwei
 *
 */
public class CounterHandler extends AbstractTextHandler {

	private static final String TAG = "统计";
	private static final String RELOAD = "reload", SERVICE_COUNT = "serviceCount";

	@Override
	public String handle(String content) {
		if(StringUtil.isBlank(content)) {
			return null;
		}
		
		if(content.startsWith(TAG)) {
			if(ServiceCounter.refresh()==false) {
				return null;
			}
			
			String cmd = content.substring(2).trim();
			Date date = DateUtil.parse(cmd);
			String day = DateUtil.format(date!=null?date:SystemClock.date(), "yyyy-MM-dd");
			//支持：统计，返回当天信息
			if(StringUtil.isBlank(cmd) || date!=null) {
				Map<String, Integer> count = ServiceCounter.counts.get(day);
				if(count!=null) {
					StringBuilder sb = new StringBuilder(day).append("\n");
					String[] names = count.keySet().toArray(new String[0]);
					Arrays.sort(names);
					int total = 0;
					for(String name : names) {
						Integer times = count.get(name);
						if(times!=null) {
							sb.append(name).append("=").append(times).append("\n");
							total += times;
						}
					}
					sb.append("total: ").append(total);
					return sb.toString();
				}
			}else if(RELOAD.equals(cmd)){
				ServiceCounter.reload();
			}else {
				return countDays(cmd);
			} 
		}else if(content.startsWith(SERVICE_COUNT)) {
			content = content.substring(SERVICE_COUNT.length());
			if(StringUtil.hasLength(content)) {
				Boolean b = NumberUtil.parseBoolean(content, null);
				if(b!=null) {
					ServiceHandler.serviceCount(b.booleanValue());
				}
			}
			return String.valueOf(ServiceHandler.serviceCount);
		}
		return null;
	}

	private String countDays(String cmd) {
		Date date;
		String day;
		boolean isNumbers = StringUtil.isNumbers(cmd);
		StringBuilder sb = new StringBuilder(cmd).append("\n");
		date = SystemClock.date();
		int days = isNumbers ? Integer.parseInt(cmd) : 30, total = 0;
		Map<String, Integer> totalCount = new HashMap<>(8);
		do {
			day = DateUtil.format(date, "yyyy-MM-dd");
			Map<String, Integer> count = ServiceCounter.counts.get(day);
			if(count!=null) {
				if(isNumbers) {
					for(String key : count.keySet()) {
						Integer times = count.get(key);
						if(times!=null) {
							Integer totalTimes = totalCount.get(key);
							if(totalTimes==null) {
								totalCount.put(key, times);
							}else {
								totalCount.put(key, totalTimes+times);
							}
						}
					}
				}else {
					Integer times = count.get(cmd);
					if(times!=null) {
						sb.append(day).append("=").append(times).append("\n");
						total += times;
					}
				}
				if(--days > 0) {
					date = DateUtils.addDays(date, -1);
					continue;
				}
			}
			sb.insert(0, (SystemClock.date().getTime()-date.getTime())/TimeUnit.DAYS.toMillis(1)+"/");
			break;
		}while(true);
		if(isNumbers) {
			PairList<String, Integer> pairList = new PairList.PriorityPairList<>(PriorityComparator.DESEND);
			pairList.putAll(totalCount);
			while(pairList.moveNext()) {
				sb.append(pairList.getData()).append("=").append(pairList.getPriority()).append("\n");
				total += pairList.getPriority();
			}
		}
		sb.append("total: ").append(total);
		return sb.toString();
	}

	private static class ServiceCounter {
		/**
		 * 保留所有redis访问统计信息property:service{day}
		 */
		public static final Map<String, Map<String, Integer>> counts = new LinkedHashMap<>();
		static {
			reload();
			//每天凌晨更新昨天数据
			TaskUtil.scheduleAtFixedRate(() -> {
					String day = DateUtil.format(DateUtils.addDays(SystemClock.date(),-1), "yyyy-MM-dd"), key = "service"+day, value = RedisConfig.get(key);
					if(StringUtil.hasLength(value)) {
						Map<String, Integer> count = ConfigUtil.stringMapInteger(value);
						if(count!=null) {
							counts.put(day, count);
						}
					}
			}, 24-DateUtils.getFragmentInHours(SystemClock.date(), Calendar.DATE), 24, TimeUnit.HOURS);
		}
		public static void reload() {
			//等3秒再加载缓存数据
			TaskUtil.schedule(() -> {
					Date date = SystemClock.date();
					String day, key, value;
					do {
						day = DateUtil.format(date, "yyyy-MM-dd");
						key = "service"+day;
						value = RedisConfig.get(key);
						if(StringUtil.hasLength(value)) {
							Map<String, Integer> count = ConfigUtil.stringMapInteger(value);
							if(count!=null) {
								counts.put(day, count);
								date = DateUtils.addDays(date, -1);
								continue;
							}
						}
						break;
					}while(true);
			}, 3, TimeUnit.SECONDS);
		}
		public static boolean refresh() {
			String day = DateUtil.format(SystemClock.date(), "yyyy-MM-dd"), key = "service"+day, value = RedisConfig.get(key);
			if(StringUtil.hasLength(value)) {
				Map<String, Integer> count = ConfigUtil.stringMapInteger(value);
				if(count!=null) {
					counts.put(day, count);
					return true;
				}
			}
			return false;
		}
	}
}
