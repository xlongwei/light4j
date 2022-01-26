package com.xlongwei.light4j.handler.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONArray;
import com.networknt.utility.StringUtils;
import com.xlongwei.light4j.apijson.DemoApplication;
import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.DateUtil;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.IdCardUtil;
import com.xlongwei.light4j.util.JsonUtil;
import com.xlongwei.light4j.util.NumberUtil;
import com.xlongwei.light4j.util.JsonUtil.JsonBuilder;
import com.xlongwei.light4j.util.PinyinUtil;
import com.xlongwei.light4j.util.StringUtil;
import com.xlongwei.light4j.util.ZhDate;

import org.apache.commons.lang3.RandomUtils;

import cn.hutool.core.date.Zodiac;
import io.undertow.server.HttpServerExchange;

/**
 * idcard util
 * @author xlongwei
 *
 */
public class IdcardHandler extends AbstractHandler {

	public void parse(HttpServerExchange exchange) throws Exception {
		String idNumber = HandlerUtil.getParam(exchange, "idNumber");
		if(StringUtil.hasLength(idNumber)) {
			Map<String, String> params = idcardInfo(idNumber);
			HandlerUtil.setResp(exchange, params);
		}
	}

	public static Map<String, String> idcardInfo(String idNumber) {
		boolean valid = StringUtil.isIdNumber(idNumber);
		Map<String, String> params = StringUtil.params("valid", String.valueOf(valid));
		if(idNumber.length()>=2) {//行政区划
			String area = idNumber.substring(0,Math.min(idNumber.length(), 6));
			if(StringUtil.isNumbers(area)) {
				String areas = StringUtil.join(IdCardUtil.areas(area), null, null, null);
				if(area.length()==6) {
					params.put("area", area);
				}
				if(!StringUtil.isBlank(areas)) {
					params.put("areas", areas);
				}
			}
		}
		boolean old = idNumber.length() == 15 && !"19".equals(idNumber.substring(6, 8));
		if(params.containsKey("area") && idNumber.length()>=10) {//出生日期
			String year = old ? "19" + idNumber.substring(6,8) : idNumber.substring(6, 10);
			String month = old ? idNumber.substring(8, 10) : (idNumber.length()>=12 ? idNumber.substring(10,12) : "01");
			String day = old ? idNumber.substring(10, 12) : (idNumber.length()>=14 ? idNumber.substring(12, 14) : "01");
			Date birth = DateUtil.parse(year+month+day);
			if(birth != null && birth.before(new Date())) {
				int age = cn.hutool.core.date.DateUtil.ageOfNow(birth);
				params.put("age", String.valueOf(age));
				params.put("year", year);
				String birthday = DateUtil.format(birth, "yyyyMMdd");
				if((!old && idNumber.length()>=14 && birthday.equals(idNumber.substring(6, 14))) || (old && idNumber.length()>=12 && birthday.endsWith(idNumber.substring(6, 12)))) {
					params.put("birth", birthday);
					params.put("zodiac", Zodiac.getZodiac(birth));
					ZhDate zhDate = ZhDate.fromDate(birth);
					if(zhDate != null) {
						params.put("nongli", zhDate.toString());
						params.put("chinese", zhDate.chinese());
						params.put("ganzhi", zhDate.ganzhi());
						params.put("shengxiao", zhDate.shengxiao());
					}
				}
			}
		}
		if(params.containsKey("birth") && (old || idNumber.length()==18)) {//序号+性别
			String serial = old ? idNumber.substring(12, 15) : (idNumber.length()==18 ? idNumber.substring(14, 17) : null);
			if(serial != null && StringUtil.isNumbers(serial)) {
				boolean male = Integer.parseInt(serial)%2==1;
				params.put("male", Boolean.toString(male));
				params.put("sex", male?"男":"女");
			}
		}else if(idNumber.length()>15 && idNumber.length()!=18) {
			params.put("error", "身份证长度有误="+idNumber.length());
		}
		return params;
	}
	
	public void valid(HttpServerExchange exchange) throws Exception {
		String idNumber = HandlerUtil.getParam(exchange, "idNumber");
		HandlerUtil.setResp(exchange, StringUtil.params("valid", Boolean.toString(StringUtil.isIdNumber(idNumber))));
	}
	
	public void fix(HttpServerExchange exchange) throws Exception {
		String idNumber = HandlerUtil.getParam(exchange, "idNumber");
		if(!StringUtil.isBlank(idNumber) && idNumber.length()==18) {
			Map<String, String> map = new HashMap<>(16);
			if(StringUtil.isIdNumber(idNumber)) {
				map.putAll(idcardInfo(idNumber));
			}else {
				int p = idNumber.indexOf('*');
				if(p==17) {
					idNumber = idNumber.substring(0, 17);
					char check = StringUtil.checkIdNumber(idNumber);
					if(check!=0) {
						idNumber = idNumber+check;
						map.put("idNumber", idNumber);
						map.putAll(idcardInfo(idNumber));
					}
				}else {
					for(int i=0;i<10;i++) {
						idNumber = idNumber.substring(0, p)+i+idNumber.substring(p+1);
						if(StringUtil.isIdNumber(idNumber)) {
							map.put("idNumber", idNumber);
							map.putAll(idcardInfo(idNumber));
							break;
						}
					}
				}
			}
			HandlerUtil.setResp(exchange, map);
		}
	}
	
	public void area(HttpServerExchange exchange) throws Exception {
		boolean children = NumberUtil.parseBoolean(HandlerUtil.getParam(exchange, "children"), false);
		if(children){
			areas(exchange);
		}else{
			String area = HandlerUtil.getParam(exchange, "area");
			Set<String> areas = new LinkedHashSet<String>(IdCardUtil.areas(area));
			if(areas.size() > 0) {
				HandlerUtil.setResp(exchange, StringUtil.params("area", StringUtil.join(areas, null, null, null)));
			}
		}
	}
	
	/** 获取省市区代码 */
	public void areas(HttpServerExchange exchange) throws Exception {
		String area = HandlerUtil.getParam(exchange, "area");
		Map<String, String> map = null;
		if(StringUtil.isBlank(area)) {
			if(DemoApplication.apijsonEnabled){
				JsonBuilder json = JsonUtil.builder(false);
				json.putJSON("Idcard[]").putJSON("Idcard").put("@column", "code,name").put("code$", "%0000").put("year", IdCardUtil.year).top().put("sort()", "pinyinSort(Idcard[],name)");
				String string  = json.top().build().toJSONString();
				string = DemoApplication.apijson.get(string, null);
				JSONArray array = JsonUtil.parseNew(string).getJSONArray("Idcard[]");
				map = new LinkedHashMap<>();
				for(int i=0,s=array==null ? 0 : array.size();i<s;i++){
					map.put(array.getJSONObject(i).getString("code").substring(0, 2),array.getJSONObject(i).getString("name"));
				}
			}else{
				map = IdCardUtil.areas.entrySet().stream()
						.filter(e -> e.getKey().endsWith("0000"))
						.sorted(Map.Entry.comparingByValue(PinyinUtil.ZH_COMPARATOR))
						.collect(Collectors.toMap(e -> e.getKey().substring(0, 2), Entry::getValue, (v1,v2) -> v1, LinkedHashMap::new));
			}
			HandlerUtil.setResp(exchange, Collections.singletonMap("areas", map));
		}else if(area.matches("\\d{2}")) {
			if(DemoApplication.apijsonEnabled){
				JsonBuilder json = JsonUtil.builder(false);
				json.putJSON("Idcard[]").putJSON("Idcard").put("@column", "code,name").put("code$", area+"%00").put("year", IdCardUtil.year).top().put("sort()", "pinyinSort(Idcard[],name)");
				String string  = json.top().build().toJSONString();
				string = DemoApplication.apijson.get(string, null);
				JSONArray array = JsonUtil.parseNew(string).getJSONArray("Idcard[]");
				map = new LinkedHashMap<>();
				for(int i=0,s=array==null ? 0 : array.size();i<s;i++){
					String code = array.getJSONObject(i).getString("code");
					if(!code.endsWith("0000") || s==1) map.put(code.substring(0, 4),array.getJSONObject(i).getString("name"));
				}
			}else{
				map = IdCardUtil.areas.entrySet().stream()
						.filter(e -> e.getKey().startsWith(area) && e.getKey().endsWith("00") && !e.getKey().endsWith("0000"))
						.sorted(Map.Entry.comparingByValue(PinyinUtil.ZH_COMPARATOR))
						.collect(Collectors.toMap(e -> e.getKey().substring(0, 4), Entry::getValue, (v1,v2) -> v1, LinkedHashMap::new));
				if(map.isEmpty()){
					map.put(area+"00", IdCardUtil.areas.get(area+"0000"));
				}
			}
			HandlerUtil.setResp(exchange, Collections.singletonMap("areas", map));
		}else if(area.matches("\\d{4}")) {
			String prefix = area.endsWith("00") ? area.substring(0, 2) : area;
			if(DemoApplication.apijsonEnabled){
				JsonBuilder json = JsonUtil.builder(false);
				json.putJSON("Idcard[]").putJSON("Idcard").put("@column", "code,name").put("code$", prefix+"%").put("year", IdCardUtil.year).top().put("sort()", "pinyinSort(Idcard[],name)");
				String string  = json.top().build().toJSONString();
				string = DemoApplication.apijson.get(string, null);
				JSONArray array = JsonUtil.parseNew(string).getJSONArray("Idcard[]");
				map = new LinkedHashMap<>();
				for(int i=0,s=array==null ? 0 : array.size();i<s;i++){
					String code = array.getJSONObject(i).getString("code");
					if(!code.endsWith("00")) map.put(code,array.getJSONObject(i).getString("name"));
				}
			}else{
				map = IdCardUtil.areas.entrySet().stream()
						.filter(e -> e.getKey().startsWith(prefix) && !e.getKey().endsWith("00"))
						.sorted(Map.Entry.comparingByValue(PinyinUtil.ZH_COMPARATOR))
						.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (v1,v2) -> v1, LinkedHashMap::new));
			}
			HandlerUtil.setResp(exchange, Collections.singletonMap("areas", map));
		}else {
			Set<String> areas = new LinkedHashSet<String>(IdCardUtil.areas(area));
			if(areas.size() > 0) {
				HandlerUtil.setResp(exchange, StringUtil.params("area", StringUtil.join(areas, null, null, null)));
			}
		}
	}

	/** 生成证件号码 */
	public void gen(HttpServerExchange exchange) throws Exception {
		String area = HandlerUtil.getParam(exchange, "area"), birthday = StringUtil.firstNotBlank(HandlerUtil.getParam(exchange, "birth"), HandlerUtil.getParam(exchange, "birthday")), serial = HandlerUtil.getParam(exchange, "serial"), sex = HandlerUtil.getParam(exchange, "sex");
		if(StringUtil.isBlank(area) || !StringUtil.isNumbers(area) || area.length()>6 || area.endsWith("00") || IdCardUtil.areas.get(area=StringUtils.rightPad(area, 6, '0'))==null) {
			String prefix = !StringUtil.isBlank(area)&&IdCardUtil.valid(area) ? (area.endsWith("0000") ? area.substring(0, 2) : (area.endsWith("00") ? area.substring(0, 4) : area)) : null;
			List<String> list = null;
			if(DemoApplication.apijsonEnabled) {
				JsonBuilder json = JsonUtil.builder(false);
				json = json.putJSON("Idcard[]").putJSON("Idcard").put("@column", "code");
				if(prefix!=null) json.put("code$", prefix+"%");
				else json.put("code!$", "%00");
				json.put("year", IdCardUtil.year).top().put("sort()", "pinyinSort(Idcard[],name)");
				String string  = json.top().build().toJSONString();
				string = DemoApplication.apijson.get(string, null);
				JSONArray array = JsonUtil.parseNew(string).getJSONArray("Idcard[]");
				list = new ArrayList<>();
				for(int i=0,s=array==null ? 0 : array.size();i<s;i++){
					list.add(array.getJSONObject(i).getString("code"));
				}
			} else {
				list = IdCardUtil.areas.entrySet().stream().filter(e -> !e.getKey().endsWith("00") && (prefix==null||e.getKey().startsWith(prefix))).map(Entry::getKey).collect(Collectors.toList());
			}
			area = list.get(RandomUtils.nextInt(0, list.size()));
		}
		if(!StringUtil.isBlank(birthday)) {
			Date date = DateUtil.parse(birthday);
			if(date != null) {
				String birth = DateUtil.format(date, "yyyyMMdd");
				if(Integer.parseInt(birth.substring(0, 4))>=1900 && birthday.endsWith(birth.substring(7, 8))) {
					birthday = birth;
				}
			}
		}
		if(StringUtil.isBlank(birthday)) {
			String year = HandlerUtil.getParam(exchange, "year"), month = HandlerUtil.getParam(exchange, "month"), day = HandlerUtil.getParam(exchange, "day");
			if(StringUtil.isBlank(year) || !year.matches("\\d{4}") || Integer.parseInt(year)<1900) {
				year = String.valueOf(RandomUtils.nextInt(1900, cn.hutool.core.date.DateUtil.date().year()));
			}
			if(StringUtil.isBlank(month) || !month.matches("(0?[1-9])|1[0-2]")) {
				int m = RandomUtils.nextInt(1, 13);
				month = m<10 ? "0"+m : String.valueOf(m);
			}else if(month.length()<2) {
				month = "0"+month;
			}
			if(StringUtil.isBlank(day) || !StringUtil.isNumbers(day) || DateUtil.parse(year+month+day)==null) {
				int d = RandomUtils.nextInt(1, cn.hutool.core.date.DateUtil.endOfMonth(new Date()).dayOfMonth());
				day = d<10 ? "0"+d : String.valueOf(d);
			}else if(day.length()<2) {
				day = "0"+day;
			}
			birthday = year + month + day;
		}
		if(StringUtil.isBlank(serial) || serial.length()!=3 || !StringUtil.isNumbers(serial)) {
			int start = 0, end = 999, s = RandomUtils.nextInt(start, end), m = "男".equals(sex) ? 1 : ("女".equals(sex) ? 0 : 2);
			while(m != 2 && s%2 != m) {
				s = RandomUtils.nextInt(start, end);
			}
			serial = s<10 ? "00"+s : (s<100 ? "0"+s : String.valueOf(s));
		}
		String idNumber = area+birthday+serial;
		char check = StringUtil.checkIdNumber(idNumber);
		if(check!=0) {
			idNumber = idNumber+check;
			Map<String, String> idcardInfo = idcardInfo(idNumber);
			idcardInfo.put("idNumber", idNumber);
			HandlerUtil.setResp(exchange, idcardInfo);
		}
	}
}
