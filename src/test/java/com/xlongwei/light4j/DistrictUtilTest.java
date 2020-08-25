package com.xlongwei.light4j;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import com.xlongwei.light4j.util.FileUtil.CharsetNames;
import com.xlongwei.light4j.util.FileUtil.LineHandler;
import com.xlongwei.light4j.util.FileUtil.TextReader;
import com.xlongwei.light4j.util.FileUtil.TextWriter;
import com.xlongwei.light4j.util.SqlInsert;
import com.xlongwei.light4j.util.StringUtil;

import cn.hutool.poi.excel.ExcelUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 行政区划代码数据处理
 * @author xlongwei
 */
@Slf4j
public class DistrictUtilTest {
	@Data
	static class District {
		private String province;
		private String provinceName;
		private String city;
		private String cityName;
		private String county;
		private String countyName;
	}
	@Test
	public void parseData() throws Exception {
		Map<String, District> parseTxt = parseTxt();
		log.info("parse txt={}", parseTxt.size());
		Map<String, District> parseXlsx = parseXlsx();
		log.info("parse xlsx={}", parseXlsx.size());
		parseTxt.entrySet().forEach(entry -> {
			String county = entry.getKey();
			District district = parseXlsx.get(county);
			if(district==null) {
				District b = entry.getValue();
				if(b.getCounty().startsWith(b.getCity().substring(0, 4))) {
					log.info("add county={}, district=\n{}", county, b);
					parseXlsx.put(county, b);
				}else {
					log.error("bad district={}", b);
				}
			}else {
				if(!district.equals(entry.getValue())) {
					District b = entry.getValue();
					if(b.getCounty().startsWith(b.getCity().substring(0, 4))) {
						log.info("district not equal\n{}\n{}", district, entry.getValue());
					}
				}
			}
		});
		log.info("available districts={}", parseXlsx.size());
		SqlInsert sqlInsert = new SqlInsert("district");
		sqlInsert.addColumns("provinceName,province,cityName,city,countyName,county".split("[,]"));
		TextWriter writer = new TextWriter("apijson/district.sql", CharsetNames.UTF_8);
		int batch = 100;
		List<String> keys = new ArrayList<>(parseXlsx.keySet());
		Collections.sort(keys);
		for(String key : keys) {
			if(sqlInsert.size() >= batch) {
				writer.writeln(sqlInsert.toString());
				sqlInsert.clear();
			}
			District d = parseXlsx.get(key);
			sqlInsert.addValues(d.getProvinceName(), d.getProvince(), d.getCityName(), d.getCity(), d.getCountyName(), d.getCounty());
		}
		if(sqlInsert.size() > 0) {
			writer.writeln(sqlInsert.toString());
		}
		writer.close();
		log.info("finish");
	}
	private Map<String, District> parseTxt() throws Exception {
		Map<String, District> parse = new HashMap<>();
		new TextReader("apijson/district.txt", CharsetNames.UTF_8).handleLines(new LineHandler() {
			String province, provinceName, city, cityName;
			@Override
			public void handle(String line) {
				if(!StringUtil.isBlank(line)) {
					String[] split = StringUtils.split(line);
					if(split==null || split.length!=2) {
						log.error("bad line={}", line);
					}else {
						String county = split[0], countyName = split[1];
						if(!StringUtil.isNumbers(county)) {
							log.error("bad line={}", line);
						}else if(county.endsWith("0000")) {
							province = city = split[0];
							provinceName = cityName = split[1];
						}else if(county.endsWith("00")) {
							city = split[0];
							cityName = split[1];
						}else {
							District district = parse.get(county);
							if(district != null) {
								log.error("repeated county={}, countyName={}", county, countyName);
							}else {
								district = new District();
								district.setProvinceName(provinceName);
								district.setProvince(province);
								district.setCityName(cityName);
								district.setCity(city);
								district.setCountyName(countyName);
								district.setCounty(county);
								parse.put(county, district);
							}
						}
					}
				}
			}
		});
		return parse;
	}
	private Map<String, District> parseXlsx() throws Exception {
		Map<String, District> parse = new HashMap<>();
		ExcelUtil.readBySax(new BufferedInputStream(new FileInputStream("apijson/district.xlsx")), 0,
				(int i, long j, List<Object> list) -> {
						Object[] array = list.toArray();
						if(array!=null && array.length==6) {
							String county = Objects.toString(array[5], null);
							if(StringUtil.isNumbers(county)) {
								District district = parse.get(county);
								if(district != null) {
									log.error("repeated county={}, row={}", county, j);
								}else {
									int idx = 0;
									district = new District();
									district.setProvinceName(Objects.toString(array[idx++], ""));
									district.setProvince(Objects.toString(array[idx++], ""));
									district.setCityName(Objects.toString(array[idx++], ""));
									district.setCity(Objects.toString(array[idx++], ""));
									district.setCountyName(Objects.toString(array[idx++], ""));
									district.setCounty(Objects.toString(array[idx++], ""));
									parse.put(county, district);
								}
							}
						}
				});
		return parse;
	}
}
