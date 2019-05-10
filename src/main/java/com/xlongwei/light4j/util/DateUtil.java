package com.xlongwei.light4j.util;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.networknt.utility.CollectionUtil;
import com.networknt.utility.StringUtils;

/**
 * 常见日期类型处理：字符串、日期Date、长整数new Date(long)，parse(time).getTime()
 * @author xlongwei
 */
public class DateUtil {
	private static Logger log = LoggerFactory.getLogger(DateUtil.class);
	private static Map<Integer, Set<String>> formats = new HashMap<>();
	private static Map<String, FastDateFormat> fastDateFormats = new HashMap<>();
	private static FastDateFormat httpHeader = FastDateFormat.getInstance("EEE, dd MMM yyyy HH:mm:ss zzz", TimeZone.getTimeZone("GMT"), Locale.US);

	static {
		String[] strings = { "yyyyMMdd", "yyyy-MM-dd", "yyyy年MM月dd日", "yyyyMMddHHmmss", "yyyy-MM-dd HH:mm:ss" };
		for(String string : strings) {
			addFormat(string);
		}
	}
	
	public static void addFormat(String format) {
		if(StringUtils.isNotBlank(format)) {
			try {
				FastDateFormat fastDateFormat = FastDateFormat.getInstance(format);
				Integer length = Integer.valueOf(format.length());
				Set<String> set = formats.get(length);
				if(set==null) {
					set = new HashSet<>();
					formats.put(length, set);
				}
				if(!set.contains(format)) {
					set.add(format);
					fastDateFormats.put(format, fastDateFormat);
				}
			}catch(Exception e) {
				log.warn("fail to add format: {}, ex: {}", format, e.getMessage());
			}
		}
	}


	/**
	 * @param time 支持格式：<li>yyyy-MM-dd, mysql: curdate()</li>
	 * <li>yyyy-MM-dd HH:mm:ss, mysql: now()</li>
	 * <li>1406167122870，java: System.currentTimeInMillis()</li>
	 * <li>1406166160，mysql: unix_timestamp(now())</li>
	 */
	public static Date parse(String time) {
		if(StringUtils.isBlank(time)) {
			return null;
		}
		Set<String> set = formats.get(time.length());
		if(!CollectionUtil.isEmpty(set)) {
			for(String format : set) {
				try {
					FastDateFormat fastDateFormat = fastDateFormats.get(format);
					return fastDateFormat.parse(time);
				}catch(Exception e) {
					//ignore
				}
			}
		}
		int length = time.length();
		boolean isLongTime = (length==10 || length==13) && time.matches("\\d+");
		if(isLongTime) {
			return new Date(length==13?Long.parseLong(time):Long.parseLong(time)*1000);
		}
		int httpHeaderLength = 29;
		if(length == httpHeaderLength) {
			try {
				return httpHeader.parse(time);
			}catch(Exception e) {
				//ignore
			}
		}
		log.info("fail to parse time: {}", time);
		return null;
	}
	
	public static String format(Date date, String format) {
		if(date==null || StringUtils.isBlank(format)) {
			return null;
		}
		FastDateFormat fastDateFormat = fastDateFormats.get(format);
		return fastDateFormat==null ? null : fastDateFormat.format(date);
	}

	public static String format(Date date) {
		return format(date, "yyyy-MM-dd HH:mm:ss");
	}
}
