package com.xlongwei.light4j;

import java.util.Calendar;
import java.util.Date;

import org.junit.Test;

import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.ChineseCalendar;

import lombok.extern.slf4j.Slf4j;

/**
 * icu4j test
 * @author xlongwei
 *
 */
@Slf4j
public class Icu4jTest {

	@Test
	public void nongli() throws Exception {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		Date date = df.parse("2011-6-6");
		ChineseCalendar cal = new ChineseCalendar(date);
		log.info("" + cal.get(ChineseCalendar.YEAR));
		log.info("" + cal.get(ChineseCalendar.MONTH));
		log.info("" + cal.get(ChineseCalendar.DATE));
		log.info("" + cal.get(ChineseCalendar.IS_LEAP_MONTH));

	}

	@Test
	public void yangli() throws Exception {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		Date date = df.parse("2011-5-5");
		ChineseCalendar cal = new ChineseCalendar(date);
		while (true) {
			int month = cal.get(ChineseCalendar.MONTH);
			int day = cal.get(ChineseCalendar.DATE);
			if (month != 4 || day != 5) {
				cal.add(ChineseCalendar.DATE, 1);
				continue;
			} else {
				Calendar cal2 = Calendar.getInstance();
				cal2.setTime(cal.getTime());
				log.info(
						cal2.get(Calendar.YEAR) + "-" + (cal2.get(Calendar.MONTH) + 1) + "-" + cal2.get(Calendar.DATE));
				break;
			}
		}
	}
}
