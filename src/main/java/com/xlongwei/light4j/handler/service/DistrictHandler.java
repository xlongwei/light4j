package com.xlongwei.light4j.handler.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONObject;
import com.networknt.httpstring.ContentType;
import com.networknt.utility.StringUtils;
import com.networknt.utility.Util;
import com.xlongwei.light4j.beetl.model.District;
import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.Http2Util;
import com.xlongwei.light4j.util.JsonUtil;
import com.xlongwei.light4j.util.JsonUtil.JsonBuilder;
import com.xlongwei.light4j.util.MySqlUtil;
import com.xlongwei.light4j.util.NumberUtil;
import com.xlongwei.light4j.util.PinyinUtil;
import com.xlongwei.light4j.util.StringUtil;

import cn.hutool.core.util.StrUtil;
import io.undertow.client.ClientRequest;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import lombok.extern.slf4j.Slf4j;

/**
 * district handler
 * @author xlongwei
 *
 */
@Slf4j
public class DistrictHandler extends AbstractHandler {

	public void provinces(HttpServerExchange exchange) throws Exception {
		List<District> provinces = MySqlUtil.SQLMANAGER.lambdaQuery(District.class).distinct().groupBy("province")/*.orderBy("provinceName")*/.select("province", "provinceName");
		HandlerUtil.setResp(exchange, Collections.singletonMap("provinces", provinces.stream().sorted(PinyinUtil.zhStringFieldComparator(District.class, "provinceName")).collect(Collectors.toList())));
	}
	
	public void cities(HttpServerExchange exchange) throws Exception {
		String province = HandlerUtil.getParam(exchange, "province");
		if(!StringUtil.isBlank(province)) {
			List<District> cities = MySqlUtil.SQLMANAGER.lambdaQuery(District.class).distinct().andEq("province", province).groupBy("city")/*.orderBy("cityName")*/.select("city", "cityName");
			HandlerUtil.setResp(exchange, Collections.singletonMap("cities", cities.stream().sorted(PinyinUtil.zhStringFieldComparator(District.class, "cityName")).collect(Collectors.toList())));
		}
	}
	
	public void counties(HttpServerExchange exchange) throws Exception {
		String city = HandlerUtil.getParam(exchange, "city");
		if(!StringUtil.isBlank(city)) {
			List<District> counties = MySqlUtil.SQLMANAGER.lambdaQuery(District.class).distinct().andEq("city", city).groupBy("county")/*.orderBy("countyName")*/.select("county", "countyName");
			HandlerUtil.setResp(exchange, Collections.singletonMap("counties", counties.stream().sorted(PinyinUtil.zhStringFieldComparator(District.class, "countyName")).collect(Collectors.toList())));
		}
	}
	
	public void county(HttpServerExchange exchange) throws Exception {
		String county = HandlerUtil.getParam(exchange, "county");
		if(!StringUtil.isBlank(county)) {
			District single = MySqlUtil.SQLMANAGER.lambdaQuery(District.class).andEq("county", county).single();
			HandlerUtil.setResp(exchange, Collections.singletonMap("county", single));
		}
	}
	
	public void query(HttpServerExchange exchange) throws Exception {
		String code = HandlerUtil.getParam(exchange, "code");
		boolean parents = NumberUtil.parseBoolean(HandlerUtil.getParam(exchange, "parents"), false);
		boolean children = NumberUtil.parseBoolean(HandlerUtil.getParam(exchange, "children"), true);
		String path = "get", request = null;
		if(StringUtil.isBlank(code) || StringUtil.isNumbers(code)) {
			String province = StrUtil.sub(code, 0, 2);
			String city = StrUtil.sub(code, 0, 4);
			String county = StrUtil.sub(code, 0, 6);
			String town = StrUtil.sub(code, 0, 9);
			String village = StrUtil.sub(code, 0, 12);
			JsonBuilder json = JsonUtil.builder(false);
			if(StringUtil.isBlank(code)) {
				json.putJSON("Province[]").putJSON("Province").top().put("sortProvince()", "pinyinSort(Province[],name)");
			}else if(code.length()==2) {
				json.put("province", province).putJSON("Province").put("code", province).top();
				if(parents) {
					json.putJSON("Province[]").putJSON("Province").top().put("sortProvince()", "pinyinSort(Province[],name)");
				}
				if(children) {
					json.putJSON("City[]").putJSON("City").put("code$", province+"%").top().put("sortCity()", "pinyinSort(City[],name)");
				}
			}else if(code.length()==4) {
				json.put("province", province).putJSON("Province").put("code", province).top();
				json.put("city", city).putJSON("City").put("code", city).top();
				if(parents) {
					json.putJSON("Province[]").putJSON("Province").top().put("sortProvince()", "pinyinSort(Province[],name)");
					json.putJSON("City[]").putJSON("City").put("code$", province+"%").top().put("sortCity()", "pinyinSort(City[],name)");
				}
				if(children) {
					json.putJSON("County[]").putJSON("County").put("code$", city+"%").top().put("sortCounty()", "pinyinSort(City[],name)");
				}
			}else if(code.length()==6) {
				json.put("province", province).putJSON("Province").put("code", province).top();
				json.put("city", city).putJSON("City").put("code", city).top();
				json.put("county", county).putJSON("County").put("code", county).top();
				if(parents) {
					json.putJSON("Province[]").putJSON("Province").top().put("sortProvince()", "pinyinSort(Province[],name)");
					json.putJSON("City[]").putJSON("City").put("code$", province+"%").top().put("sortCity()", "pinyinSort(City[],name)");
					json.putJSON("County[]").putJSON("County").put("code$", city+"%").top().put("sortCounty()", "pinyinSort(County[],name)");
				}
				if(children) {
					json.putJSON("Town[]").putJSON("Town").put("code$", county+"%").top().put("sortTown()", "pinyinSort(Town[],name)");
				}
			}else if(code.length()==9) {
				json.put("province", province).putJSON("Province").put("code", province).top();
				json.put("city", city).putJSON("City").put("code", city).top();
				json.put("county", county).putJSON("County").put("code", county).top();
				json.put("town", county).putJSON("Town").put("code", town).top();
				if(parents) {
					json.putJSON("Province[]").putJSON("Province").top().put("sortProvince()", "pinyinSort(Province[],name)");
					json.putJSON("City[]").putJSON("City").put("code$", province+"%").top().put("sortCity()", "pinyinSort(City[],name)");
					json.putJSON("County[]").putJSON("County").put("code$", city+"%").top().put("sortCounty()", "pinyinSort(County[],name)");
					json.putJSON("Town[]").putJSON("Town").put("code$", county+"%").top().put("sortTown()", "pinyinSort(Town[],name)");
				}
				if(children) {
					json.putJSON("Village[]").putJSON("Village").put("code$", town+"%").top().put("sortVillage()", "pinyinSort(Village[],name)");
				}
			}else if(code.length()==12) {
				json.put("province", province).putJSON("Province").put("code", province).top();
				json.put("city", city).putJSON("City").put("code", city).top();
				json.put("county", county).putJSON("County").put("code", county).top();
				json.put("town", county).putJSON("Town").put("code", town).top();
				json.put("village", village).putJSON("Village").put("code", village).top();
				if(parents) {
					json.putJSON("Province[]").putJSON("Province").top().put("sortProvince()", "pinyinSort(Province[],name)");
					json.putJSON("City[]").putJSON("City").put("code$", province+"%").top().put("sortCity()", "pinyinSort(City[],name)");
					json.putJSON("County[]").putJSON("County").put("code$", city+"%").top().put("sortCounty()", "pinyinSort(County[],name)");
					json.putJSON("Town[]").putJSON("Town").put("code$", county+"%").top().put("sortTown()", "pinyinSort(Town[],name)");
					json.putJSON("Village[]").putJSON("Village").put("code$", town+"%").top().put("sortVillage()", "pinyinSort(Village[],name)");
				}
			}
			request = json.build().toJSONString();
		}
		if(request != null) {
			log.info(request);
			ApijsonHandler.apijson(path, request, exchange);
		}
	}

	private static Map<String, String> codeLengthMapTable = new LinkedHashMap<>();
	static {
		codeLengthMapTable.put("2", "province");
		codeLengthMapTable.put("4", "city");
		codeLengthMapTable.put("6", "county");
		codeLengthMapTable.put("9", "town");
		codeLengthMapTable.put("12", "village");
	}

	public void search(HttpServerExchange exchange) throws Exception {
		String name = HandlerUtil.getParam(exchange, "name");
		if(StringUtil.isBlank(name) || !StringUtil.isChinese(name)){
			return;
		}
		String type = HandlerUtil.getParam(exchange, "type");
		if (type != null && !type.isEmpty()) {
			final String finalType = type;
			type = codeLengthMapTable.entrySet().stream().filter(entry -> entry.getValue().equals(finalType)).map(entry -> entry.getKey()).findFirst().orElse(null);
		}
		String ancestor = StringUtils.trimToEmpty(HandlerUtil.getParam(exchange, "ancestor"));
		int num = 10, n = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "n"), num);
		n = n > 0 ? n : num;
		ClientRequest clientRequest = new ClientRequest();
		clientRequest.setMethod(Methods.GET);
		clientRequest.setPath("/service/district/search");
		clientRequest.getRequestHeaders().put(Headers.HOST, "localhost");
		clientRequest.getRequestHeaders().put(Headers.CONTENT_TYPE,
				ContentType.APPLICATION_FORM_URLENCODED_VALUE.value());
		clientRequest.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
		URI uri = new URI(System.getProperty("light-search", "http://localhost:9200"));
		String response = Http2Util.execute(uri, clientRequest, "name=" + Util.urlEncode(name) + "&length="
				+ StringUtils.trimToEmpty(type) + "&ancestor=" + ancestor + "&n=" + n);
		JSONObject json = JsonUtil.parseNew(response);
		if (json.containsKey("codes")) {
			String[] codes = json.getJSONArray("codes").toArray(new String[0]);
			Map<String, String> codeNames = new HashMap<>();
			codeLengthMapTable.entrySet().forEach(entry -> {
				int length = Integer.parseInt(entry.getKey());
				String table = codeLengthMapTable.get(entry.getKey());
				List<String> list = Arrays.stream(codes).filter(code -> code.length() >= length)
						.map(code -> code.substring(0, length)).collect(Collectors.toList());
				if (list != null && list.size() > 0) {
					MySqlUtil.SQLMANAGER.execute("select code,name from " + table + " where code in ( #{join(codes)} )",
							Map.class, Collections.singletonMap("codes", list)).stream().forEach(map -> {
								codeNames.put((String) map.get("code"), (String) map.get("name"));
							});
				}
			});
			List<Map<String, String>> list = new ArrayList<>();
			for (String code : codes) {
				StringBuilder fullName = new StringBuilder();
				codeLengthMapTable.entrySet().forEach(entry -> {
					int length = Integer.parseInt(entry.getKey());
					if (code.length() >= length) {
						fullName.append(codeNames.get(code.subSequence(0, length)));
					}
				});
				Map<String, String> item = new HashMap<>();
				item.put("code", code);
				item.put("name", codeNames.get(code));
				item.put("fullName", fullName.toString());
				item.put("type", codeLengthMapTable.get(String.valueOf(code.length())));
				list.add(item);
			}
			HandlerUtil.setResp(exchange, Collections.singletonMap("search", list));
		}
	}
}
