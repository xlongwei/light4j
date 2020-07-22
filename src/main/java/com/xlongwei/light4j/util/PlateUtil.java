package com.xlongwei.light4j.util;

import java.io.BufferedInputStream;
import java.util.ArrayList;
import java.util.List;

import cn.hutool.core.util.CharUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.sax.handler.RowHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * plate util
 * @author xlongwei
 * @date 2019-10-30
 */
@Slf4j
public class PlateUtil {
	
	/** 搜索城市或车牌 */
	public static String search(String str) {
		if(str==null || (str=str.trim().toUpperCase()).length()<PLATE_PREFIX_LENGTH) {
			return null;
		}
		if(CharUtil.isLetter(str.charAt(1))) {
			if(str.matches(YUN_A_V)) {
				return "云南省东川区";
			}else {
				str = str.substring(0, 2);
			}
		}
		String guess = null;
		for(String[] row : plates) {
			if(row[3].equals(str)) {
				//川X ==》 四川省广安市
				return row[0]+row[1];
			}else if(row[1].startsWith(str)) {
				//广安 ==》 川X
				return row[3];
			}else if(row[0].charAt(0)==str.charAt(0)) {
				//四川广安 ==》 川X
				int idx = 1, max = Math.min(row[0].length(), str.length());
				while(idx<max && row[0].charAt(idx)==str.charAt(idx)) {
					idx++;
				}
				String str2 = str.substring(idx);
				if(idx>1 && row[1].startsWith(str2)) {
					return row[3];
				}
			}else if(str.length()==2 && str.charAt(0)==row[3].charAt(0)) {
				//渝D ==》 重庆市
				guess = row[0];
			}
		}
		return guess;
	}

	/** List[province,city,nick,plate] */
	static List<String[]> plates = new ArrayList<>();
	static final int PLATE_PREFIX_LENGTH = 2;
	static final String YUN_A_V = "云A-?V[0-9A-Z]{0,4}";
	static {
		try{
			ExcelUtil.readBySax(new BufferedInputStream(ConfigUtil.stream("chepaihao.xlsx")), 0, new RowHandler() {
				@Override
				public void handle(int i, long j, List<Object> list) {
					if(j > 0) {
						plates.add(list.toArray(new String[list.size()]));
					}
				}
			});
			log.info("chepaihao loaded, record={}", plates.size());
		}catch(Exception e) {
			log.warn("fail to init chepaihao: {}", e.getMessage());
		}
	}
}
