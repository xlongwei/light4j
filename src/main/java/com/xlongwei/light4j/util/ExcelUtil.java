package com.xlongwei.light4j.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import lombok.extern.slf4j.Slf4j;

/**
 * excel util
 * @author xlongwei
 *
 */
@Slf4j
public class ExcelUtil {
	public static final String CONTENT_TYPE = "application/vnd.ms-excel";
	
	/** 创建内存表格
	 * @param xlsx true创建Office 2007以上类型表格，false创建Office 2003类型表格
	 */
	public static Workbook create(boolean xlsx) {
		Workbook wb = null;
		if(xlsx) {
			wb = new XSSFWorkbook();
		} else {
			wb = new HSSFWorkbook();
		}
		wb.createSheet();
		return wb;
	}
	
	/** 获取单张表sheet */
	public static Sheet sheet(Workbook wb, int i, String name) {
		if(i < 0) {
			return null;
		}
		Sheet sheet = null;
		do {
			try{
				sheet=wb.getSheetAt(i);
				if(!StringUtil.isBlank(name) && !name.equals(sheet.getSheetName())) {
					wb.setSheetName(i, name);
				}
			}catch(Exception e) { if(StringUtil.isBlank(name)) {
				wb.createSheet();
			} else {
				wb.createSheet(name);
			} }
		}while(sheet==null);
		return sheet;
	}
	
	/** 写入单张表数据 */
	public static boolean data(Sheet sheet, List<String[]> data) {
		if(data==null || data.size()==0) {
			return false;
		}
		int r = 0, col = 0; Map<Integer, Integer> widths = new HashMap<>(8);
		for(String[] arr : data) {
			Row row = sheet.createRow(r++);
			for(String item:arr) {
				Integer width = widths.get(col);
				if(width==null) {
					width = 0;
				}
				int columnWidth = columnWidth(item);
				if(columnWidth>width) {
					widths.put(col, columnWidth);
				}
				row.createCell(col++).setCellValue(item);
			}
			col = 0;
		}
		for(Integer column : widths.keySet()) {
			Integer width = widths.get(column);
			//参数的单位是1/256个字符宽度
			sheet.setColumnWidth(column, width * 256);
		}
		return true;
	}
	
	/** 表格写入文件 */
	public static boolean write(Workbook wb, File file) {
		try{
			FileOutputStream out = new FileOutputStream(file);
			wb.write(out);
			out.close();
			log.info("write xls to: "+file.getAbsolutePath());
			return true;
		}catch(Exception e) {
			log.warn("fail to write xls: "+file.getAbsolutePath()+", ex: "+e.getMessage());
			return false;
		}
	}
	
	/** 获取表格字节 */
	public static byte[] bytes(Workbook wb) {
		try{
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			wb.write(os);
			wb.close();
			return os.toByteArray();
		}catch(Exception e) {
			log.warn("fail to bytes xls: "+e.getMessage());
		}
		return null;
	}
	
	/** 封装excel单行数据或title */
	public static String[] row(String ... row) {
		return row;
	}
	
	/** 封装excel多行数据 */
	public static List<String[]> rows(String[] ... rows) {
		List<String[]> data = new ArrayList<>();
		if(rows!=null && rows.length>0) {
			for(String[] row:rows) {
				data.add(row);
			}
		}
		return data;
	}
	
	/** 列序号转为列名称，1 => A，26 => Z，27 => AA */
	public static String columnName(int number) {
		if(number <= 0) {
			return null;
		}
		StringBuilder name=new StringBuilder();
		int m=0,r=26;
		do {
			m=(number-1)%r;
			number=(number-1)/r;
			name.append((char)('A'+m));
		}while(number>0);
		return name.reverse().toString();
	}
	
	/** 列名称转为列序号：A => 1，Z => 26，AA => 27*/
	public static int columnNumber(String name) {
		if(StringUtil.hasLength(name)) {
			char[] chars = name.toCharArray();
			int sum = 0, len=chars.length;
			for(int i=0;i<len;i++) {
				int n = chars[i]-'A';
				sum *= 26;
				sum += n+1;
			}
			return sum;
		}
		return 0;
	}
	
	/** 计算字符串显示宽度 */
	public static int columnWidth(String value) {
		if(value==null || value.length()==0) {
			return 0;
		}
		char[] cs = value.toCharArray();
		int width = 0;
		for(char c : cs) {
			width += StringUtil.isChinese(c) ? 2 : 1;
		}
		return Math.min(width+1, 255);
	}
}
