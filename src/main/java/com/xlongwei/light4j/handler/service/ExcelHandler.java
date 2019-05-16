package com.xlongwei.light4j.handler.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.ExcelUtil;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.IdWorker;
import com.xlongwei.light4j.util.JsonUtil;
import com.xlongwei.light4j.util.NumberUtil;
import com.xlongwei.light4j.util.StringUtil;
import com.xlongwei.light4j.util.UploadUtil;

import io.undertow.server.HttpServerExchange;

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
	
	public void width(HttpServerExchange exchange) throws Exception {
		String value = HandlerUtil.getParam(exchange, "value");
		int width = ExcelUtil.columnWidth(value);
		HandlerUtil.setResp(exchange, StringUtil.params("width", String.valueOf(width * 256)));
	}

}
