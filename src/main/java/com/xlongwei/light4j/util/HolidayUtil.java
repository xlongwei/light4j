package com.xlongwei.light4j.util;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateRange;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.Week;
import cn.hutool.core.date.format.FastDateFormat;
import lombok.extern.slf4j.Slf4j;

/**
 * 节假日判断
 * @author xlongwei
 */
@Slf4j
public class HolidayUtil {
	public static Map<String, String> plans = new HashMap<>(32);
	public static Map<String, Integer> holidays = new HashMap<>(64);
	public static FastDateFormat dateFormat = FastDateFormat.getInstance("yyyy.MM.dd");
	private static String days2020 = "{\"元旦节\":\"1.1\",\"春节\":\"-1.19,1.24-2.2\",\"清明节\":\"4.4-6\",\"劳动节\":\"-4.26,5.1-5,-5.9\",\"端午节\":\"6.25-27,-6.28\",\"国庆节\":\"-9.27,10.1-8,-10.10\"}";
	private static String days2019 = "{\"元旦节\":\"1.1\",\"春节\":\"-2.2-3,2.4-10\",\"清明节\":\"4.5\",\"劳动节\":\"5.1\",\"端午节\":\"6.7\",\"中秋节\":\"9.13\",\"国庆节\":\"-9.29,10.1-7,-10.12\"}";
	private static String days2018 = "{\"元旦节\":\"1.1\",\"春节\":\"-2.11,2.15-21,-2.24\",\"清明节\":\"4.5-7,-4.8\",\"劳动节\":\"-4.28,4.29-5.1\",\"端午节\":\"6.18\",\"中秋节\":\"9.24\",\"国庆节\":\"-9.29-30,10.1-7\"}";
	private static FastDateFormat dayFormat = FastDateFormat.getInstance("M月d日");
	
	static {
		addPlan("2020", days2020);
		addPlan("2019", days2019);
		addPlan("2018", days2018);
	}
	
	/**
	 * 判断是否工作日
	 * @param day
	 * @return true是工作日 false节假日或周末
	 */
	public static boolean isworkday(Date day) {
		Integer flag = holidays.get(dateFormat.format(day));
		if(flag != null) {
			return flag.intValue() < 0;
		}else {
			return !isweekend(day);
		}
	}
	
	/**
	 * 判断是否节假日
	 * @param day
	 * @return
	 */
	public static boolean isholiday(Date day) {
		Integer flag = holidays.get(dateFormat.format(day));
		return flag!=null && flag.intValue()>0;
	}
	
	/**
	 * 判断是否周末
	 * @param day
	 * @return
	 */
	public static boolean isweekend(Date day) {
		Week week = DateUtil.dayOfWeekEnum(day);
		return week==Week.SATURDAY || week==Week.SUNDAY;
	}
	
	/**
	 * 返回下个工作日（如果day是工作日则返回day）
	 * @param day
	 * @return
	 */
	public static Date nextworkday(Date day) {
		return nextworkday(day, false);
	}
	
	/**
	 * 返回下个工作日（如果day是工作日则返回day）
	 * @param day
	 * @param skipweekend true跳过周末
	 */
	public static Date nextworkday(Date day, boolean skipweekend) {
		while(isworkday(day)==false || (skipweekend && isweekend(day))) {
			day = DateUtil.offsetDay(day, 1);
		}
		return day;
	}
	
	/**
	 * 返回某天加减N个工作日
	 * @param day
	 * @param offset
	 * @return
	 */
	public static Date offsetworkday(Date day, int offset) {
		return offsetworkday(day, offset, false);
	}
	
	/**
	 * 返回某天加减N个工作日
	 * @param day
	 * @param offset
	 * @param skipweekend true跳过周末
	 */
	public static Date offsetworkday(Date day, int offset, boolean skipweekend) {
		if(offset==0) {
			return nextworkday(day, skipweekend);
		}else {
			int step = offset>0 ? 1 : -1;
			offset = Math.abs(offset);
			while(offset>0) {
				day = DateUtil.offsetDay(day, step);
				while(isworkday(day)==false || (skipweekend && isweekend(day))) {
					day = DateUtil.offsetDay(day, step);
				}
				offset -= 1;
			}
			return day;
		}
	}
	
	/**
	 * 计算两个日期之间的工作日数量
	 * @param start
	 * @param end
	 * @return
	 */
	public static int betweenworkday(Date start, Date end, boolean skipweekend) {
		int workdays = 0;
		DateRange range = start.after(end) ? DateUtil.range(end, start, DateField.DAY_OF_MONTH)
				: DateUtil.range(start, end, DateField.DAY_OF_MONTH);
		Iterator<DateTime> iterator = range.iterator();
		while(iterator.hasNext()) {
			DateTime next = iterator.next();
			if(isworkday(next) && !(skipweekend && isweekend(next))) {
				workdays++;
			}
		}
		return workdays;
	}
	
	/**
	 * 获取flag对应的节日
	 * @param flag 1
	 * @return 元旦节
	 */
	public static String nameOf(int flag) {
		if(flag > 0) {
			Holiday[] values = Holiday.values();
			if(flag<=values.length) {
				return values[flag-1].name();
			}
		}
		return null;
	}
	
	/**
	 * 添加某年的节假日计划
	 * @param year 2020
	 * @param plan {"春节":"-1.19,1.24-30,-2.1"}
	 * @descr 减号开头表示调班，减号分隔表示连休
	 */
	public static void addPlan(String year, String plan) {
		if(StringUtil.isNumbers(year)==false || year.length()!=4) {
			return;
		}
		JSONObject json = JsonUtil.parseNew(plan);
		for(String key : json.keySet()) {
			String value = json.getString(key);
			Holiday holiday = Holiday.nameOf(key);
			if(StringUtil.isBlank(value) || holiday==null) {
				continue;
			}
			int serial = holiday.ordinal()+1;
			//春节=-1.19,1.24-2.2
			String[] split = value.split("[,]");
			for(String day : split) {
				boolean isworkday = false;
				if(day.charAt(0)=='-') {
					day = day.substring(1);
					isworkday = true;
				}
				int pos = day.indexOf('-'), flag = isworkday ? -serial : serial;
				if(pos==-1) {
					Date date = parse(year+"."+day);
					if(date==null) {
						log.info("bad holiday config, year={}, holiday={}, day={}", year, key, day);
						continue;
					}else {
						holidays.put(dateFormat.format(date), flag);
						if(isworkday==false) {
							plans.put(year+"."+holiday.name(), dayFormat.format(date));
						}
					}
				}else {
					String from = day.substring(0, pos), to = day.substring(pos+1);
					int p1 = from.indexOf('.'), p2 = to.indexOf('.');
					if(p1==-1) {
						log.info("bad holiday config, year={}, holiday={}, day={}", year, key, day);
						break;
					}else if(p2==-1) {
						to = from.substring(0, p1)+"."+to;
					}
					Date fromDay = parse(year+"."+from), toDay = parse(year+"."+to);
					if(fromDay==null || toDay==null) {
						log.info("bad holiday config, year={}, holiday={}, day={}", year, key, day);
						break;
					}else {
						DateRange dateRange = DateUtil.range(fromDay, toDay, DateField.DAY_OF_MONTH);
						dateRange.forEachRemaining(dt -> {
							holidays.put(dateFormat.format(dt), flag);
						});
						if(isworkday==false) {
							plans.put(year+"."+holiday.name(), new StringBuilder(dayFormat.format(fromDay)).append("至").append(dayFormat.format(toDay)).toString());
						}
					}
				}
			}
		}
	}
	
	private static Date parse(String day) {
		try {
			return dateFormat.parse(day);
		}catch(Exception e) {
			return null;
		}
	}

	/**
	 * 节假日枚举
	 * @author xlongwei
	 */
	public enum Holiday {
		元旦节, 春节, 清明节, 劳动节, 端午节, 国庆节, 中秋节;
		public static Holiday nameOf(String name) {
			try {
				return Holiday.valueOf(name);
			}catch(Exception e) {
				return null;
			}
		}
	}
}
