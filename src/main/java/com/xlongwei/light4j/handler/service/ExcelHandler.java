package com.xlongwei.light4j.handler.service;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.Base64;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.ExcelUtil;
import com.xlongwei.light4j.util.FileUtil;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.IdWorker;
import com.xlongwei.light4j.util.ImageUtil;
import com.xlongwei.light4j.util.JsonUtil;
import com.xlongwei.light4j.util.NumberUtil;
import com.xlongwei.light4j.util.StringUtil;
import com.xlongwei.light4j.util.UploadUtil;

import cn.hutool.core.map.MapUtil;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData.FormValue;

/**
 * excel handler
 * @author xlongwei
 *
 */
@SuppressWarnings({ "rawtypes" })
public class ExcelHandler extends AbstractHandler {

	public void create(HttpServerExchange exchange) throws Exception {
		String[] title = JsonUtil.parse(HandlerUtil.getParam(exchange, "title"), String[].class);
		List rows = JsonUtil.parse(HandlerUtil.getParam(exchange, "rows"), List.class);
		List<String[]> data = new ArrayList<>();
		if(title!=null && title.length>0) {
			data.add(title);
		}
		if(rows!=null && rows.size()>0) {
			for(Object obj : rows) {
				String[] row = JsonUtil.parse(obj.toString(), String[].class);
				if(row!=null) {
					data.add(row);
				}
			}
		}
		if(data.isEmpty()) {
			return;
		}
		boolean xlsx = NumberUtil.parseBoolean(HandlerUtil.getParam(exchange, "xlsx"), false);
		Workbook workbook = ExcelUtil.create(xlsx);
		Sheet sheet = ExcelUtil.sheet(workbook, 0, HandlerUtil.getParam(exchange, "sheet"));
		ExcelUtil.data(sheet, data);
		Boolean base64File = NumberUtil.parseBoolean(HandlerUtil.getParam(exchange, "base64File"), true);
		if(base64File) {
			HandlerUtil.setResp(exchange, StringUtil.params("base64", Base64.encodeBase64String(ExcelUtil.bytes(workbook))));
		}else {
			String toPath = "excel/"+IdWorker.getId()+(xlsx?".xlsx":".xls");
			File toFile = new File(UploadUtil.SAVE_TEMP, toPath);
			ExcelUtil.write(workbook, toFile);
			HandlerUtil.setResp(exchange, StringUtil.params(UploadUtil.DOMAIN, UploadUtil.URL_TEMP, UploadUtil.PATH, toPath));
		}
	}

	public void column(HttpServerExchange exchange) throws Exception {
		String name = HandlerUtil.getParam(exchange, "name");
		int number = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "number"), 0);
		if(StringUtil.isBlank(name)) {
			name = ExcelUtil.columnName(number);
		}else {
			number = ExcelUtil.columnNumber(name);
		}
		HandlerUtil.setResp(exchange, StringUtil.params("name", name, "number", String.valueOf(number)));
	}
	
	public void read(HttpServerExchange exchange) throws Exception {
		InputStream is = null;
		String url = HandlerUtil.getParam(exchange, "url");
		if(StringUtil.isUrl(url)) {
			is = FileUtil.stream(url);
		}
		if(is == null) {
			String base64 = HandlerUtil.getParam(exchange, "base64");
			base64 = StringUtil.isBlank(base64) ? null : ImageUtil.prefixRemove(base64);
			if(!StringUtil.isBlank(base64)) {
				byte[] bs = Base64.decodeBase64(base64);
				if(bs != null) {
					is = new ByteArrayInputStream(bs);
				}
			}
		}
		if(is == null) {
			FormValue file = HandlerUtil.getFile(exchange, "file");
			if(file!=null && file.isFileItem()) {
				is = file.getFileItem().getInputStream();
			}
		}
		if(is != null) {
			int sheetNo = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "sheetNo"), 0);
			int headLine = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "headLine"), 0);
			String sheetName = HandlerUtil.getParam(exchange, "sheetName");
			Map<String, Object> map = MapUtil.newHashMap(true);
			List<Object> list = new ArrayList<>();
			ExcelReader excelReader = EasyExcel.read(new BufferedInputStream(is)).autoCloseStream(true).registerReadListener(new AnalysisEventListener<Map<Integer, String>>() {
				@Override
				public void invoke(Map<Integer, String> data, AnalysisContext context) {
					list.add(data.entrySet().stream().map(Entry::getValue).collect(Collectors.toList()));
				}
				@Override
				public void doAfterAllAnalysed(AnalysisContext context) {
				}
			}).build();
			if("*".equals(sheetName)) {
				List<ReadSheet> sheetList = excelReader.excelExecutor().sheetList();
				map.put("size", sheetList.size());
				map.put("sheetNames", sheetList.stream().map(ReadSheet::getSheetName).collect(Collectors.toList()));
				sheetList.forEach(sheet -> {
					list.clear();
					excelReader.read(sheet);
					HashMap<String, Object> sheetMap = MapUtil.newHashMap();
					sheetMap.put("size", list.size());
					sheetMap.put("data", new ArrayList<>(list));
					map.put(sheet.getSheetName(), sheetMap);
				});
			}else {
				ReadSheet sheet = new ReadSheet(StringUtil.isBlank(sheetName)?sheetNo:null, StringUtil.nullOrString(sheetName));
				sheet.setHeadRowNumber(headLine);
				excelReader.read(sheet);
				map.put("size", list.size());
				map.put("data", list);
			}
			excelReader.finish();
			HandlerUtil.setResp(exchange, map);
		}
	}
	
	public void width(HttpServerExchange exchange) throws Exception {
		String value = HandlerUtil.getParam(exchange, "value");
		int width = ExcelUtil.columnWidth(value);
		HandlerUtil.setResp(exchange, StringUtil.params("width", String.valueOf(width * 256)));
	}

}
