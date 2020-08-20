package com.xlongwei.light4j.handler.service;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.FileUtil;
import com.xlongwei.light4j.util.FileUtil.CharsetNames;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.NumberUtil;
import com.xlongwei.light4j.util.PdnovelUtil;
import com.xlongwei.light4j.util.StringUtil;
import com.xlongwei.light4j.util.UploadUtil;
import com.xlongwei.light4j.util.PdnovelUtil.Book;
import com.xlongwei.light4j.util.PdnovelUtil.Chapter;
import com.xlongwei.light4j.util.PdnovelUtil.Volume;

import io.undertow.server.HttpServerExchange;

/**
 * pdnovel handler
 * @author xlongwei
 *
 */
public class PdnovelHandler extends AbstractHandler {

	public void books(HttpServerExchange exchange) throws Exception {
		String type = HandlerUtil.getParam(exchange, "type");
		if("reload".equals(type)) {
			PdnovelUtil.reload();
		}else if("merge".equals(type)) {
			int novelid = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "novelid"), 0);
			int volumeid = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "volumeid"), 0);
			PdnovelUtil.merge(novelid, volumeid);
		}
		Map<String, Object> map = new HashMap<>(4);
		JSONArray books = new JSONArray();
		for(Entry<Integer, Book> entry : PdnovelUtil.books.entrySet()) {
			Integer novelid = entry.getKey();
			Book book = entry.getValue();
			JSONObject item = new JSONObject();
			item.put("novelid", novelid);
			item.put("name", book.name);
			books.add(item);
		}
		map.put("books", books);
		HandlerUtil.setResp(exchange, map);
	}
	
	public void volumes(HttpServerExchange exchange) throws Exception {
		Integer novelid = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "novelid"), null);
		if(novelid != null) {
			Book book = PdnovelUtil.books.get(novelid);
			if(book != null) {
				Map<String, Object> map = new HashMap<>(1);
				JSONArray volumes = new JSONArray();
				for(Entry<Integer, Volume> entry : book.volumes.entrySet()) {
					Integer volumeid = entry.getKey();
					Volume volume = entry.getValue();
					JSONObject item = new JSONObject();
					item.put("volumeid", volumeid);
					item.put("volumeorder", volume.volumeorder);
					item.put("volumename", volume.volumename);
					volumes.add(item);
				}
				map.put("volumes", volumes);
				HandlerUtil.setResp(exchange, map);
			}
		}
	}
	
	public void chapters(HttpServerExchange exchange) throws Exception {
		Integer novelid = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "novelid"), null);
		Integer volumeid = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "volumeid"), null);
		if(novelid!=null && volumeid!=null) {
			Book book = PdnovelUtil.books.get(novelid);
			if(book != null) {
				Volume volume = book.volumes.get(volumeid);
				if(volume != null) {
					Map<String, Object> map = new HashMap<>(1);
					JSONArray chapters = new JSONArray();
					for(Entry<Integer, Chapter> entry : volume.chapters.entrySet()) {
						Chapter chapter = entry.getValue();
						JSONObject item = new JSONObject();
						item.put("chapterorder", chapter.chapterorder);
						item.put("chaptername", chapter.chaptername);
						item.put("chapterurl", UploadUtil.URL+"pdnovel/chapter/"+novelid+"/"+volumeid+"/"+chapter.chapterorder+".txt");
						chapters.add(item);
					}
					map.put("chapters", chapters);
					HandlerUtil.setResp(exchange, map);
				}
			}
		}
	}
	
	public void chapter(HttpServerExchange exchange) throws Exception {
		Integer novelid = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "novelid"), null);
		Integer volumeid = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "volumeid"), null);
		Integer chapterorder = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "chapterorder"), null);
		if(novelid==null || volumeid==null || chapterorder==null) {
			return;
		}
		String path = new StringBuilder("chapter/").append(novelid).append("/").append(volumeid).append("/").append(chapterorder).append(".txt").toString();
		File file = new File(UploadUtil.SAVE+"pdnovel/"+path);
		if(file.exists()) {
			String content = FileUtil.readString(file, CharsetNames.UTF_8);
			HandlerUtil.setResp(exchange, StringUtil.params("content", content));
		}
	}

}
