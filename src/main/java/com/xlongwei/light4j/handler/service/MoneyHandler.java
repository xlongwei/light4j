package com.xlongwei.light4j.handler.service;

import java.util.LinkedHashMap;
import java.util.Map;

import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.NumberUtil;
import com.xlongwei.light4j.util.StringUtil;

import cn.hutool.core.map.MapUtil;
import io.undertow.server.HttpServerExchange;

/**
 * money handler
 * @author xlongwei
 *
 */
public class MoneyHandler extends AbstractHandler {

	private static final int MONTHS = 12;
	private static final String BX = "BX";
	private static final String MY = "MY";
	private static final String BY = "BY";

	public void daxie(HttpServerExchange exchange) throws Exception {
		String amount = HandlerUtil.getParam(exchange, "amount");
		String daxie = NumberUtil.daxie(amount);
		HandlerUtil.setResp(exchange, StringUtil.params("daxie", daxie));
	}
	
	public void exp(HttpServerExchange exchange) throws Exception {
		String exp = HandlerUtil.getParam(exchange, "exp");
		Map<String, Number> context = MapUtil.newHashMap();
		HandlerUtil.getParamNames(exchange).stream().forEach(paramName -> {
			String paramValue = HandlerUtil.getParam(exchange, paramName);
			Number number = StringUtil.isNumbers(paramValue) ? NumberUtil.parseInt(paramValue, null) : null;
			if(number == null && StringUtil.isDecimal(paramValue)) {
				number = NumberUtil.parseDouble(paramValue, null);
			}
			if(number != null) {
				context.put(paramName, number);
			}
		});
		String result = NumberUtil.parseExp(exp, context);
		HandlerUtil.setResp(exchange, StringUtil.params("result", result));
	}
	
	public void loan(HttpServerExchange exchange) throws Exception {
		String a = HandlerUtil.getParam(exchange, "A"), b = HandlerUtil.getParam(exchange, "B"), m = HandlerUtil.getParam(exchange, "M"), x = HandlerUtil.getParam(exchange, "X");
		if(!StringUtil.hasLength(b)) { double by = NumberUtil.parseDouble(HandlerUtil.getParam(exchange, BY), 0.0); if(by>0.0) {
			b = String.valueOf(by / 12.0);
		} }
		if(!StringUtil.hasLength(m)) { int my = NumberUtil.parseInt(HandlerUtil.getParam(exchange, MY), 0); if(my>0) { m = String.valueOf(my * MONTHS); } }
		if(!StringUtil.isDecimal(a) || !StringUtil.isNumbers(m)) {
			return;
		}
		// 默认等额本息
		boolean bx = NumberUtil.parseBoolean(HandlerUtil.getParam(exchange, BX), true); 
		Map<String, String> map = new LinkedHashMap<>();
		if(bx) {
			if(!StringUtil.isDecimal(b) && !StringUtil.isDecimal(x)) {
				return;
			}
			String t; // (1+B)^M
			if(StringUtil.hasLength(b)) {
				t = NumberUtil.parseExp("(1+"+b+")^"+m);
				x = NumberUtil.parseExp(a+"*("+b+"*"+t+")/("+t+"-1)");
				map.put("X", NumberUtil.format(Double.valueOf(x), "0.00"));
				String xt = NumberUtil.parseExp(x+"*"+m+"-"+a);
				map.put("XT", NumberUtil.format(Double.valueOf(xt), "0.00"));
			}else {
				double bl = 0.000001, bh = 0.999999, bt = 0.0, xt = Double.parseDouble(x), xi = 0.0, xTimes = 36.0, delta = 0.0001;
				if(xt*Integer.parseInt(m) < Double.parseDouble(a)) {
					//不够本金，何谈利息
					return; 
				}
				do {
					bt = (bl+bh)/2.0;
					t = NumberUtil.parseExp("(1+"+bt+")^"+m);
					xi = Double.parseDouble(NumberUtil.parseExp(a+"*("+bt+"*"+t+")/("+t+"-1)"));
					if(Math.abs(xi-xt)<delta) {
						break;
					}
					if(xi>xt) {
						bh = bt;
					} else {
						bl = bt;
					}
					xTimes -= 1.0;
				}while(xTimes > 0.0);
				map.put("B", NumberUtil.format(bt, "0.000000"));
				map.put(BY, NumberUtil.format(bt*12, "0.0000"));
			}
		}else {
			int xm = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "XM"), 0);
			int mt = Integer.parseInt(m);
			//每期偿还本金
			String xa = NumberUtil.parseExp(a+"/"+m+".0");
			if(StringUtil.isDecimal(b)) {
				if(xm > 0) {
					x = NumberUtil.parseExp(xa+"+("+m+"-"+xm+"+1)*"+xa+"*"+b);
					map.put("X"+xm, NumberUtil.format(Double.valueOf(x), "0.00"));
				}else {
					for(int i=1;i<=mt;i++) {
						x = NumberUtil.parseExp(xa+"+("+m+"-"+i+"+1)*"+xa+"*"+b);
						map.put("X"+i, NumberUtil.format(Double.valueOf(x), "0.00"));
					}
					//总利息=〔(总贷款额÷还款月数+总贷款额×月利率)+总贷款额÷还款月数×(1+月利率)〕÷2×还款月数-总贷款额
					String xt = NumberUtil.parseExp("("+xa+"+"+a+"*"+b+"+"+xa+"*(1+"+b+"))/2*"+m+"-"+a);
					map.put("XT", NumberUtil.format(Double.valueOf(xt), "0.00"));
				}
			}else {
				if(xm<1 || xm>mt || !StringUtil.isDecimal(x)) {
					return;
				}
				b = NumberUtil.parseExp("("+x+"-"+xa+")/("+mt+"-"+xm+"+1)/"+xa);
				map.put("B", NumberUtil.format(Double.valueOf(b), "0.000000"));
				map.put(BY, NumberUtil.format(Double.valueOf(b)*12, "0.0000"));
			}
		}
		HandlerUtil.setResp(exchange, map);
	}

}
