package com.xlongwei.light4j;

import org.junit.Assert;
import org.junit.Test;

import com.xlongwei.light4j.util.QnObject;
import com.xlongwei.light4j.util.QnObject.QnException;

/**
 * QnObject测试类
 * @author xlongwei
 *
 */
public class QnObjectTest {

	@Test public void test1() {
		QnObject qnObj = QnObject.fromString("您好：{姓名}(性别=男)[先生](性别=女)[女士]");
		System.out.println(qnObj.toString());
		System.out.println(QnObject.toJs(qnObj));
	}
	
	@Test public void test2() {
		QnObject qnObj = QnObject.fromString("您好：{姓名}(性别=男)[(年龄>=60)[老]先生]");
		System.out.println(qnObj.toString());
		System.out.println(QnObject.toJs(qnObj));
	}
	
	@Test public void test3() {
		QnObject qnObj = QnObject.fromString("您好：{姓名}(性别=男 and 年龄>=60)[老先生]");
		System.out.println(qnObj.toString());
		System.out.println(QnObject.toJs(qnObj));
	}
	
	@Test public void test4() {
		QnObject qnObj = QnObject.fromString("您好：{姓名}(({性别}=男 and {年龄}>=60) or {机构}!=北京)[老先生]");
		System.out.println(qnObj.toString());
		System.out.println(QnObject.toJs(qnObj));
	}
	
	@Test public void testFails() {
		String qn = null;
		try {
			qn = "您好：{姓名(性别=男 and 年龄>=60)[老先生]";
			QnObject.fromString(qn);
			Assert.fail();
		}catch(QnException e) {
			System.out.println(qn);
			System.out.println(qn.substring(0, e.getPos())+" => "+e.getMessage());
			Assert.assertEquals(QnException.MISS_VAR_END, e.getMessage());
		}
		try {
			qn = "您好：{姓名}(性别=男 and 年龄>=60)老先生]";
			QnObject.fromString(qn);
			Assert.fail();
		}catch(QnException e) {
			System.out.println(qn);
			System.out.println(qn.substring(0, e.getPos())+" => "+e.getMessage());
			Assert.assertEquals(QnException.MISS_CONDITION_INNER, e.getMessage());
		}
		try {
			qn = "您好：{}(性别=男 and 年龄>=60)[老先生]";
			QnObject.fromString(qn);
			Assert.fail();
		}catch(QnException e) {
			System.out.println(qn);
			System.out.println(qn.substring(0, e.getPos())+" => "+e.getMessage());
			Assert.assertEquals(QnException.EMPTY_VAR, e.getMessage());
		}
		try {
			qn = "您好：{姓名}(性别=男 and 年龄>=60)[老先生";
			QnObject.fromString(qn);
			Assert.fail();
		}catch(QnException e) {
			System.out.println(qn);
			System.out.println(qn.substring(0, e.getPos())+" => "+e.getMessage());
			Assert.assertEquals(QnException.MISS_CONDITION_END, e.getMessage());
		}
		try {
			qn = "您好：{姓名}({性别}=男 and {年龄}>=60 or 机构!=北京)[老先生]";
			QnObject.fromString(qn);
			Assert.fail();
		}catch(QnException e) {
			System.out.println(qn);
			System.out.println(qn.substring(0, e.getPos())+" => "+e.getMessage());
			Assert.assertEquals(QnException.BAD_AND_OR, e.getMessage());
		}
	}
}
