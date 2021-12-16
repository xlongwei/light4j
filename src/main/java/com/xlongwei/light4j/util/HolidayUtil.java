package com.xlongwei.light4j.util;

import java.time.Month;
import java.time.Year;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import com.networknt.config.Config;

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
	public static final Map<String, Integer> holidays = new HashMap<>(2048);
	public static final FastDateFormat dateFormat = FastDateFormat.getInstance("yyyy.MM.dd");
	public static final FastDateFormat weekFormat = FastDateFormat.getInstance("MM.F.u");
	public static final String[] solarTerm = {"小寒","大寒","立春","雨水","惊蛰","春分","清明","谷雨","立夏","小满","芒种","夏至","小暑","大暑","立秋","处暑","白露","秋分","寒露","霜降","立冬","小雪","大雪","冬至"};
	public static final Map<String, String> solarFestivals = new LinkedHashMap<>();
	public static final Map<String, String> lularFestivals = new LinkedHashMap<>();
	public static final Map<String, String> monthWeeks = new LinkedHashMap<>();
	private static final FastDateFormat dayFormat = FastDateFormat.getInstance("M月d日");
	
	static {
		loadFromConfig();
	}

	@SuppressWarnings("unchecked")
	private static void loadFromConfig() {
		Map<String, Object> map = Config.getInstance().getJsonMapConfig("datetime");
		HolidayUtil.lularFestivals.putAll((Map<String,String>)map.get("lularFestivals"));
		HolidayUtil.solarFestivals.putAll((Map<String,String>)map.get("solarFestivals"));
		HolidayUtil.monthWeeks.putAll((Map<String,String>)map.get("monthWeeks"));
		((Map<Object,Object>)map.get("holidays")).forEach((k,v)->{
			try{
				String json=Config.getInstance().getMapper().writeValueAsString(v);
				HolidayUtil.addPlan(k.toString(), json);
			}catch(Exception e){
				log.warn(e.getMessage());
			}
		});
		map.clear();
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
		} else if ("05.01".equals(monthDay)) {
			return Holiday.劳动节;// Integer.parseInt(format.substring(0, 4))>=1889
		} else if ("10.01".equals(monthDay)) {
			return Holiday.国庆节;// Integer.parseInt(format.substring(0, 4))>=1949
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
			String yearMonth = format.substring(5), week = null;
			String remark = solarFestivals.get(yearMonth);//阳历节日
			if(remark == null) {
				ZhDate zhDate = ZhDate.fromDate(day);
				if(zhDate != null) {
					String chinese = zhDate.chinese();
					yearMonth = chinese.substring(chinese.indexOf('年')+1);
					remark = lularFestivals.get(yearMonth);//农历节日
				}
			}
			if(remark == null) {
				yearMonth = format.substring(5);
				remark = solarFestivals.get(yearMonth);//阳历节日
			}
			if(remark == null) {
				yearMonth = weekFormat.format(day);
				remark = monthWeeks.get(yearMonth);
				week = yearMonth.substring(yearMonth.lastIndexOf(".")+1);
			}
			if(remark == null && month == 3 && "1".equals(week)) {
				int length = Month.MARCH.length(Year.of(year).isLeap());
				if(length - date < 7) {
					remark = "中小学安全教育日";
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
