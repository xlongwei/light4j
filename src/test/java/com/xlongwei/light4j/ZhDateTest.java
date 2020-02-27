package com.xlongwei.light4j;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import com.xlongwei.light4j.util.DateUtil;
import com.xlongwei.light4j.util.ZhDate;

/**
 * 农历日期类测试
 * @author xlongwei
 *
 */
public class ZhDateTest {
	public static boolean True = true, False = false;

	@Test public void validate() {
		Assert.assertTrue(ZhDate.validate(1900, 11, 1, False));
		Assert.assertTrue(ZhDate.validate(1900, 11, 29, False));
		Assert.assertTrue(ZhDate.validate(1900, 12, 30, False));
		Assert.assertTrue(ZhDate.validate(2001, 12, 29, False));
		Assert.assertTrue(ZhDate.validate(1909, 2, 29, True));
		Assert.assertTrue(ZhDate.validate(1933, 5, 30, False));
		Assert.assertFalse(ZhDate.validate(1909, 1, 29, True));
		Assert.assertFalse(ZhDate.validate(1909, 2, 30, True));
		Assert.assertFalse(ZhDate.validate(2100, 12, 29, True));
		Assert.assertTrue(ZhDate.validate(2100, 12, 29, False));
		Assert.assertFalse(ZhDate.validate(2100, 12, 30, True));
	}
	
	@Test public void decode() {
		Assert.assertEquals(Arrays.asList(29, 30, 29, 29, 30, 29, 30, 30, 29, 30, 30, 29, 30), ZhDate.decode(19416));
		Assert.assertEquals(Arrays.asList(29, 30, 29, 29, 30, 29, 30, 29, 30, 30, 30, 29), ZhDate.decode(19168));
		Assert.assertEquals(Arrays.asList(29, 30, 29, 30, 29, 29, 30, 29, 29, 30, 30, 29, 30), ZhDate.decode(21717));
		Assert.assertEquals(Arrays.asList(29, 30, 30, 29, 30, 29, 30, 29, 30, 29, 30, 29, 30), ZhDate.decode(91476));
		Assert.assertEquals(Arrays.asList(30, 30, 29, 30, 29, 30, 29, 30, 29, 29, 30, 29, 30), ZhDate.decode(119381));
	}
	
	@Test public void toDate() {
		assertEquals(new ZhDate(2100, 11, 27).toDate(), datetime(2100, 12, 27));
		assertEquals(new ZhDate(1903, 5, 17).toDate(), datetime(1903, 6, 12));
		assertEquals(new ZhDate(1903, 5, 17, True).toDate(), datetime(1903, 7, 11));
		assertEquals(new ZhDate(1900, 1, 20).toDate(), datetime(1900, 2, 19));
		assertEquals(new ZhDate(2050, 1, 28).toDate(), datetime(2050, 2, 19));
		assertEquals(new ZhDate(2050, 3, 30, True).toDate(), datetime(2050, 5, 20));
		assertEquals(new ZhDate(1900, 1, 1).toDate(), datetime(1900, 1, 31));
	}
	
	@Test public void fromDate() {
		Assert.assertEquals(ZhDate.fromDate(datetime(2100, 12, 27)), new ZhDate(2100, 11, 27));
		Assert.assertEquals(ZhDate.fromDate(datetime(1903, 6, 12)), new ZhDate(1903, 5, 17));
		Assert.assertEquals(ZhDate.fromDate(datetime(1903, 7, 11)), new ZhDate(1903, 5, 17, True));
		Assert.assertEquals(ZhDate.fromDate(datetime(1900, 2, 19)), new ZhDate(1900, 1, 20));
		Assert.assertEquals(ZhDate.fromDate(datetime(2050, 2, 19)), new ZhDate(2050, 1, 28));
		Assert.assertEquals(ZhDate.fromDate(datetime(2050, 5, 20)), new ZhDate(2050, 3, 30, True));
		Assert.assertEquals(ZhDate.fromDate(datetime(1900, 1, 31)), new ZhDate(1900, 1, 1));
	}
	
	@Test public void test() {
		Assert.assertEquals("庚子", new ZhDate(1900, 9, 1, False).ganzhi());
		Assert.assertEquals("鼠", new ZhDate(1900, 9, 1, False).shengxiao());
		Assert.assertEquals("一九零零年九月初一", new ZhDate(1900, 9, 1, False).chinese());
		Assert.assertEquals("二零二零年正月二十八", ZhDate.fromDate(DateUtil.parse("2020-02-21")).chinese());
		Assert.assertEquals("鼠", ZhDate.shengxiao(1900));
		Assert.assertEquals("猪", ZhDate.shengxiao(1899));
	}
	
	private Date datetime(int year, int month, int day) {
		Calendar c = Calendar.getInstance();
		c.set(year, month-1, day, 0, 0, 0);
		return c.getTime();
	}
	private void assertEquals(Date d1, Date d2) {
		Assert.assertEquals(DateUtil.format(d1), DateUtil.format(d2));
	}
}
