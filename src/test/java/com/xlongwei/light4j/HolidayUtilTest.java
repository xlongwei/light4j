package com.xlongwei.light4j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.xlongwei.light4j.util.DateUtil;
import com.xlongwei.light4j.util.HolidayUtil;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;

/**
 * holiday util test
 * @author xlongwei
 *
 */
public class HolidayUtilTest {
	
	@Test public void test() throws Exception {
		new TreeMap<>(HolidayUtil.holidays).forEach((k,v) -> System.out.println(k+"="+v));
		new TreeMap<>(HolidayUtil.plans).forEach((k,v) -> System.out.println(k+"="+v));
		assertEquals("元旦节", HolidayUtil.nameOf(1));
		assertEquals("春节", HolidayUtil.nameOf(2));
		assertEquals("中秋节", HolidayUtil.nameOf(7));
	}

	@Test
	public void abnormal() {
		String year = "2021";
		String plan = "{\"元旦节\":\"1.1-3\",\"春节\":\"2.11-17,-2.7,-2.20\",\"清明节\":\"4.3-5\",\"劳动节\":\"5.1-5,-4.25,-5.8\",\"端午节\":\"6.12-14\",\"中秋节\":\"-9.18,9.19-21\",\"国庆节\":\"-9.26,10.1-7,-10.9\"}";
		HolidayUtil.addPlan(year, plan);
		Date date = DateUtil.parse(year + "0101");
		Date end = DateUtil.parse(year + "1231");
		List<Date> weekendWorks = new ArrayList<>();
		List<Date> weekdayBreaks = new ArrayList<>();
		do {
			boolean isweekend = HolidayUtil.isweekend(date);
			boolean isworkday = HolidayUtil.isworkday(date);
			if (isweekend && isworkday) {
				weekendWorks.add(date);
			} else if (!isweekend && !isworkday) {
				weekdayBreaks.add(date);
			}
			date = DateUtils.addDays(date, 1);
		} while (date.before(end));
		System.out.println("weekend workdays");
		System.out.println(String.join(",",
				weekendWorks.stream().map(cn.hutool.core.date.DateUtil::formatDate).collect(Collectors.toList())));
		System.out.println("weekday breakdays");
		System.out.println(String.join(",",
				weekdayBreaks.stream().map(cn.hutool.core.date.DateUtil::formatDate).collect(Collectors.toList())));
	}

	@Test public void isworkday() throws Exception {
		assertFalse(HolidayUtil.isworkday(DateUtil.parse("2020-01-01")));
		assertTrue(HolidayUtil.isworkday(DateUtil.parse("2020-01-02")));
		assertTrue(HolidayUtil.isworkday(DateUtil.parse("2020-01-19")));
		assertFalse(HolidayUtil.isworkday(DateUtil.parse("2020-01-24")));
		assertFalse(HolidayUtil.isworkday(DateUtil.parse("2020-01-25")));
		assertFalse(HolidayUtil.isworkday(DateUtil.parse("2020-01-26")));
		assertFalse(HolidayUtil.isworkday(DateUtil.parse("2020-01-27")));
		assertFalse(HolidayUtil.isworkday(DateUtil.parse("2020-01-28")));
		assertFalse(HolidayUtil.isworkday(DateUtil.parse("2020-01-29")));
		assertFalse(HolidayUtil.isworkday(DateUtil.parse("2020-01-30")));
		assertFalse(HolidayUtil.isworkday(DateUtil.parse("2020-01-31")));
		assertFalse(HolidayUtil.isworkday(DateUtil.parse("2020-02-01")));
		assertFalse(HolidayUtil.isworkday(DateUtil.parse("2020-02-02")));
		assertTrue(HolidayUtil.isworkday(DateUtil.parse("2020-02-03")));
	}
	
	@Test public void nextworkday() throws Exception {
		assertEquals("2020-01-02", DateUtil.dateFormat.format(HolidayUtil.nextworkday(DateUtil.parse("2020-01-01"))));
		assertEquals("2020-01-20", DateUtil.dateFormat.format(HolidayUtil.nextworkday(DateUtil.parse("2020-01-18"), true)));
		assertEquals("2020-01-23", DateUtil.dateFormat.format(HolidayUtil.nextworkday(DateUtil.parse("2020-01-23"))));
		assertEquals("2020-02-03", DateUtil.dateFormat.format(HolidayUtil.nextworkday(DateUtil.parse("2020-01-24"))));
		assertEquals("2020-02-03", DateUtil.dateFormat.format(HolidayUtil.nextworkday(DateUtil.parse("2020-01-30"))));
		assertEquals("2020-02-03", DateUtil.dateFormat.format(HolidayUtil.nextworkday(DateUtil.parse("2020-02-01"))));
		assertEquals("2020-02-03", DateUtil.dateFormat.format(HolidayUtil.nextworkday(DateUtil.parse("2020-02-02"))));
		assertEquals("2020-02-03", DateUtil.dateFormat.format(HolidayUtil.nextworkday(DateUtil.parse("2020-02-01"), true)));
	}
	
	@Test public void offsetworkday() throws Exception {
		assertEquals("2020-01-02", DateUtil.dateFormat.format(HolidayUtil.offsetworkday(DateUtil.parse("2020-01-01"), 1)));
		assertEquals("2020-01-19", DateUtil.dateFormat.format(HolidayUtil.offsetworkday(DateUtil.parse("2020-01-18"), 1)));
		assertEquals("2020-01-20", DateUtil.dateFormat.format(HolidayUtil.offsetworkday(DateUtil.parse("2020-01-17"), 1, true)));
		assertEquals("2020-01-20", DateUtil.dateFormat.format(HolidayUtil.offsetworkday(DateUtil.parse("2020-01-18"), 1, true)));
		assertEquals("2020-02-03", DateUtil.dateFormat.format(HolidayUtil.offsetworkday(DateUtil.parse("2020-01-23"), 1)));
		assertEquals("2020-02-03", DateUtil.dateFormat.format(HolidayUtil.offsetworkday(DateUtil.parse("2020-01-30"), 1)));
		assertEquals("2020-02-03", DateUtil.dateFormat.format(HolidayUtil.offsetworkday(DateUtil.parse("2020-01-31"), 1)));
		assertEquals("2020-02-04", DateUtil.dateFormat.format(HolidayUtil.offsetworkday(DateUtil.parse("2020-01-30"), 2)));
		assertEquals("2020-01-23", DateUtil.dateFormat.format(HolidayUtil.offsetworkday(DateUtil.parse("2020-02-03"), -1)));
		assertEquals("2020-01-23", DateUtil.dateFormat.format(HolidayUtil.offsetworkday(DateUtil.parse("2020-02-08"), -6)));
		assertEquals("2020-02-07", DateUtil.dateFormat.format(HolidayUtil.offsetworkday(DateUtil.parse("2020-02-09"), -1)));
		assertEquals("2020-01-23", DateUtil.dateFormat.format(HolidayUtil.offsetworkday(DateUtil.parse("2020-02-09"), -6)));
		assertEquals("2020-01-19", DateUtil.dateFormat.format(HolidayUtil.offsetworkday(DateUtil.parse("2020-01-31"), -5)));
		assertEquals("2020-02-03", DateUtil.dateFormat.format(HolidayUtil.offsetworkday(DateUtil.parse("2020-01-31"), 1, true)));
	}
	
	@Test public void betweenworkday() {
		assertEquals(25, HolidayUtil.betweenworkday(DateUtil.parse("2020-01-01"), DateUtil.parse("2020-02-12"), false));
		assertEquals(24, HolidayUtil.betweenworkday(DateUtil.parse("2020-01-01"), DateUtil.parse("2020-02-12"), true));
	}
	
	@Test public void test2017To2011() {
		assertTrue(HolidayUtil.isworkday(DateUtil.parse("2011-10-8")));
		assertTrue(HolidayUtil.isworkday(DateUtil.parse("2011-12-31")));
		assertTrue(HolidayUtil.isworkday(DateUtil.parse("2012-4-1")));
		assertTrue(HolidayUtil.isworkday(DateUtil.parse("2013-4-27")));
		assertTrue(HolidayUtil.isworkday(DateUtil.parse("2014-5-4")));
		assertTrue(HolidayUtil.isworkday(DateUtil.parse("2015-2-15")));
		assertTrue(HolidayUtil.isworkday(DateUtil.parse("2016-9-18")));
		assertTrue(HolidayUtil.isworkday(DateUtil.parse("2017-4-1")));
	}
}
