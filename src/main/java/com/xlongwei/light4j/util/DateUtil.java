package com.xlongwei.light4j.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.lang3.time.FastDateFormat;

import com.networknt.utility.StringUtils;
import com.xlongwei.light4j.util.IdWorker.SystemClock;

import lombok.extern.slf4j.Slf4j;

/**
 * 常见日期类型处理：字符串、日期Date、长整数new Date(long)，parse(time).getTime()
 * 
 * @author xlongwei
 */
@Slf4j
public class DateUtil {
	/** yyyy */
	public static final FastDateFormat yearFormat = FastDateFormat.getInstance("yyyy");
	/** yyyy年M月d日 */
	public static final FastDateFormat dayFormat = FastDateFormat.getInstance("yyyy年M月d日");
	/** yyyy-MM-dd */
	public static final FastDateFormat dateFormat = FastDateFormat.getInstance("yyyy-MM-dd");
	/** yyyy-MM-dd HH:mm:ss */
	public static final FastDateFormat datetimeFormat = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");
	public static final FastDateFormat httpHeader = FastDateFormat.getInstance("EEE, dd MMM yyyy HH:mm:ss zzz",
			TimeZone.getTimeZone("GMT"), Locale.US);
	private static final List<FastDateFormat> fastDateFormats = new ArrayList<>();

	static {
		String[] strings = { "yyyyMMdd", "yyyy-MM-dd", "yyyy.MM.dd", "yyyy/MM/dd", "yyyyMMddHHmmss",
				"yyyy-MM-dd HH:mm:ss", "yyyy年MM月dd日", "yy年MM月dd日" };
		for (String string : strings) {
			addFormat(string);
		}
	}

	/**
	 * 添加日期格式
	 * @param format
	 */
	public static void addFormat(String format) {
		try {
			FastDateFormat fastDateFormat = FastDateFormat.getInstance(format);
			fastDateFormats.add(fastDateFormat);
		} catch (Exception e) {
			log.warn("fail to add format: {}, ex: {}", format, e.getMessage());
		}
	}

	/**
	 * @param datetime
	 *            支持格式：
	 *            <li>yyyy-MM-dd, mysql: curdate()</li>
	 *            <li>yyyy-MM-dd HH:mm:ss, mysql: now()</li>
	 *            <li>1406167122870，java: System.currentTimeInMillis()</li>
	 *            <li>1406166160，mysql: unix_timestamp(now())</li>
	 */
	public static Date parse(String datetime) {
		if (StringUtils.isBlank(datetime)) {
			return null;
		}
		for (FastDateFormat df : fastDateFormats) {
			try {
				return df.parse(datetime);
			} catch (Exception e) {
				// ignore
			}
		}
		int length = datetime.length();
		boolean isLongTime = (length == 10 || length == 13) && datetime.matches("\\d+");
		if (isLongTime) {
			return new Date(length == 13 ? Long.parseLong(datetime) : Long.parseLong(datetime) * 1000);
		}
		int httpHeaderLength = 29;
		if (length == httpHeaderLength) {
			try {
				return httpHeader.parse(datetime);
			} catch (Exception e) {
				// ignore
			}
		}
		log.info("fail to parse time: {}", datetime);
		return null;
	}
	
	/**
	 * 解析日期，或返回当前日期
	 * @param datetime
	 * @return
	 */
	public static Date parseNow(String datetime) {
		Date date = parse(datetime);
		if(date != null) {
			return date;
		}else {
			return SystemClock.date();
		}
	}

	/**
	 * 
	 * @param date
	 * @param format yyyy-MM-dd HH:mm:ss
	 */
	public static String format(Date date, String format) {
		if (date == null || StringUtils.isBlank(format)) {
			return null;
		}
		return FastDateFormat.getInstance(format).format(date);
	}

	/**
	 * @param date
	 * @return yyyy-MM-dd HH:mm:ss
	 */
	public static String format(Date date) {
		return datetimeFormat.format(date);
	}
}
