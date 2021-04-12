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
import cn.hutool.core.date.chinese.SolarTerms;
import cn.hutool.core.date.format.FastDateFormat;
import lombok.extern.slf4j.Slf4j;

/**
 * 节假日判断
 * @author xlongwei
 */
@Slf4j
public class HolidayUtil {
	public static final Map<String, String> plans = new HashMap<>(32);
	public static final Map<String, Integer> holidays = new HashMap<>(64);
	public static final FastDateFormat dateFormat = FastDateFormat.getInstance("yyyy.MM.dd");
	public static final String[] solarTerm = {"小寒","大寒","立春","雨水","惊蛰","春分","清明","谷雨","立夏","小满","芒种","夏至","小暑","大暑","立秋","处暑","白露","秋分","寒露","霜降","立冬","小雪","大雪","冬至"};
	public static final Map<String, String> solarFestivals = StringUtil.params("02.14","情人节","03.08","妇女节","03.12","植树节","04.01","愚人节","05.04","青年节","05.12","护士节","06.01","儿童节","07.01","建党节","08.01","建军节","09.10","教师节","12.24","平安夜","12.25","圣诞节");
	public static final Map<String, String> lularFestivals = StringUtil.params("正月十五","元宵节","二月初二","龙抬头","三月初三","上巳节","七月初七","七夕节","七月十五","中元节","九月初九","重阳节","十月十五","下元节","腊月初八","腊八节","腊月三十","除夕");
	private static final String days2020 = "{\"元旦节\":\"1.1\",\"春节\":\"-1.19,1.24-2.2\",\"清明节\":\"4.4-6\",\"劳动节\":\"-4.26,5.1-5,-5.9\",\"端午节\":\"6.25-27,-6.28\",\"国庆节\":\"-9.27,10.1-8,-10.10\"}";
	private static final String days2019 = "{\"元旦节\":\"1.1\",\"春节\":\"-2.2-3,2.4-10\",\"清明节\":\"4.5\",\"劳动节\":\"5.1\",\"端午节\":\"6.7\",\"中秋节\":\"9.13\",\"国庆节\":\"-9.29,10.1-7,-10.12\"}";
	private static final String days2018 = "{\"元旦节\":\"1.1\",\"春节\":\"-2.11,2.15-21,-2.24\",\"清明节\":\"4.5-7,-4.8\",\"劳动节\":\"-4.28,4.29-5.1\",\"端午节\":\"6.18\",\"中秋节\":\"9.24\",\"国庆节\":\"-9.29-30,10.1-7\"}";
	private static final String days2017 = "{\"元旦节\":\"1.1-2\",\"春节\":\"-1.22,1.27-2.2,-2.4\",\"清明节\":\"-4.1,4.2-4\",\"劳动节\":\"5.1\",\"端午节\":\"-5.27,5.28-30\",\"中秋节\":\"-9.30,10.4\",\"国庆节\":\"10.1-8\"}";
	private static final String days2016 = "{\"元旦节\":\"1.1\",\"春节\":\"-2.6,2.7-13,-2.14\",\"清明节\":\"4.4\",\"劳动节\":\"5.1-2\",\"端午节\":\"-6.12,6.9-11\",\"中秋节\":\"-9.18,9.15-17\",\"国庆节\":\"10.1-7,-10.8-9\"}";
	private static final String days2015 = "{\"元旦节\":\"1.1-3,-1.4\",\"春节\":\"-2.15,2.18-24,-2.28\",\"清明节\":\"4.5-6\",\"劳动节\":\"5.1\",\"端午节\":\"6.20,6.22\",\"中秋节\":\"9.27\",\"国庆节\":\"10.1-7,-10.10\"}";
	private static final String days2014 = "{\"元旦节\":\"1.1\",\"春节\":\"-1.26,1.31-2.6,-2.8\",\"清明节\":\"4.5,4.7\",\"劳动节\":\"5.1-3,-5.4\",\"端午节\":\"6.2\",\"中秋节\":\"9.8\",\"国庆节\":\"-9.28,10.1-7,-10.11\"}";
	private static final String days2013 = "{\"元旦节\":\"1.1-3,-1.5-6\",\"春节\":\"2.9-15,-2.16-17\",\"清明节\":\"4.4-6,-4.7\",\"劳动节\":\"-4.27-28,4.29-5.1\",\"端午节\":\"-6.8-9,6.10-12\",\"中秋节\":\"9.19-21,-9.22\",\"国庆节\":\"-9.29,10.1-7,-10.12\"}";
	private static final String days2012 = "{\"元旦节\":\"1.1-3\",\"春节\":\"-1.21,1.22-28,-1.29\",\"清明节\":\"4.2-4,-3.31-4.1\",\"劳动节\":\"-4.28,4.29-5.1\",\"端午节\":\"6.22-24\",\"中秋节\":\"-9.29,9.30\",\"国庆节\":\"10.1-7\"}";
	private static final String days2011 = "{\"元旦节\":\"1.1-3,-12.31\",\"春节\":\"-1.30,2.2-8,-2.12\",\"清明节\":\"-4.2,4.3-5\",\"劳动节\":\"4.30-5.2\",\"端午节\":\"6.4-6\",\"中秋节\":\"9.10-12\",\"国庆节\":\"10.1-7,-10.8-9\"}";
	private static final FastDateFormat dayFormat = FastDateFormat.getInstance("M月d日");
	
	static {
		addPlan("2020", days2020);
		addPlan("2019", days2019);
		addPlan("2018", days2018);
		addPlan("2017", days2017);
		addPlan("2016", days2016);
		addPlan("2015", days2015);
		addPlan("2014", days2014);
		addPlan("2013", days2013);
		addPlan("2012", days2012);
		addPlan("2011", days2011);
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
			return !isweekend(day) && null==guessHoliday(day);
		}
	}
	
	/**
	 * 判断是否节假日
	 * @param day
	 * @return
	 */
	public static boolean isholiday(Date day) {
		Integer flag = holidays.get(dateFormat.format(day));
		return (flag!=null && flag.intValue()>0) || null!=guessHoliday(day);
	}
	
	/**
	 * 尝试猜测法定节假日
	 * @param day
	 * @return
	 */
	public static Holiday guessHoliday(Date day) {
		String format = dateFormat.format(day);
		String monthDay = format.substring(5);
		if("01.01".equals(monthDay)) {
			return Holiday.元旦节;
		}else if("05.01".equals(monthDay) && Integer.parseInt(format.substring(0, 4))>=1889) {
			return Holiday.劳动节;
		}else if("10.01".equals(monthDay) && Integer.parseInt(format.substring(0, 4))>=1949) {
			return Holiday.国庆节;
		}
		ZhDate zhDate = ZhDate.fromDate(day);
		if(zhDate != null) {
			if(zhDate.getLunarMonth()==1 && zhDate.getLunarDay()==1) {
				return Holiday.春节;
			}else if(zhDate.getLunarMonth()==5 && zhDate.getLunarDay()==5) {
				return Holiday.端午节;
			}else if(zhDate.getLunarMonth()==8 && zhDate.getLunarDay()==15) {
				return Holiday.中秋节;
			}
		}
		//清明为24节气之一，4月第一个节为清明
		if("04".equals(format.substring(5, 7))) {
			int term = SolarTerms.getTerm(Integer.parseInt(format.substring(0, 4)), 2*4-1);
			if(term == Integer.parseInt(format.substring(8))) {
				return Holiday.清明节;
			}
		}
		return null;
	}
	
	/** 尝试猜测24节气和常见节日（非法定放假） */
	public static String guessRemark(Date day) {
		String format = dateFormat.format(day);
		int year = Integer.parseInt(format.substring(0, 4)), month = Integer.parseInt(format.substring(5, 7)), date = Integer.parseInt(format.substring(8));
		if(date == SolarTerms.getTerm(year, 2*month-1)) {
			return solarTerm[2*month-2];
		}else if(date == SolarTerms.getTerm(year, 2*month)) {
			return solarTerm[2*month-1];
		}else {
			String yearMonth = format.substring(5);
			String remark = solarFestivals.get(yearMonth);//阳历节日
			if(remark == null) {
				ZhDate zhDate = ZhDate.fromDate(day);
				if(zhDate != null) {
					String chinese = zhDate.chinese();
					yearMonth = chinese.substring(chinese.indexOf('年')+1);
					remark = lularFestivals.get(yearMonth);//农历节日
				}
			}
			return remark;
		}
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
		/**
		 * 1.1元旦节
		 */
		元旦节, 
		/**
		 * 农历除夕春节
		 */
		春节, 
		/**
		 * 二十四节气有清明节
		 */
		清明节, 
		/**
		 * 5.1劳动节
		 */
		劳动节, 
		/**
		 * 五五端午节
		 */
		端午节, 
		/**
		 * 10.1国庆节
		 */
		国庆节, 
		/**
		 * 八月十五中秋节
		 */
		中秋节;
		public static Holiday nameOf(String name) {
			try {
				return Holiday.valueOf(name);
			}catch(Exception e) {
				return null;
			}
		}
	}
}
