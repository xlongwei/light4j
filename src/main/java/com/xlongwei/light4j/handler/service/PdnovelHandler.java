package com.xlongwei.light4j.handler.service;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.xlongwei.light4j.apijson.DemoApplication;
import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.FileUtil;
import com.xlongwei.light4j.util.FileUtil.CharsetNames;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.MySqlUtil;
import com.xlongwei.light4j.util.NumberUtil;
import com.xlongwei.light4j.util.PdnovelUtil;
import com.xlongwei.light4j.util.PdnovelUtil.Book;
import com.xlongwei.light4j.util.PdnovelUtil.Chapter;
import com.xlongwei.light4j.util.PdnovelUtil.Volume;
import com.xlongwei.light4j.util.StringUtil;
import com.xlongwei.light4j.util.UploadUtil;

import org.beetl.sql.core.SQLReady;

import io.undertow.server.HttpServerExchange;

/**
 * pdnovel handler
 * @author xlongwei
 *
 */
public class PdnovelHandler extends AbstractHandler {
	static boolean loadFromFile = !DemoApplication.apijsonEnabled;

	public void books(HttpServerExchange exchange) throws Exception {
		Map<String, Object> map = new HashMap<>(4);
		JSONArray books = new JSONArray();
		if(loadFromFile){
			String type = HandlerUtil.getParam(exchange, "type");
			if("reload".equals(type)) {
				PdnovelUtil.reload();
			}else if("merge".equals(type)) {
				int novelid = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "novelid"), 0);
				int volumeid = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "volumeid"), 0);
				PdnovelUtil.merge(novelid, volumeid);
			}
			for(Entry<Integer, Book> entry : PdnovelUtil.books.entrySet()) {
				Integer novelid = entry.getKey();
				Book book = entry.getValue();
				JSONObject item = new JSONObject();
				item.put("novelid", novelid);
				item.put("name", book.name);
				books.add(item);
			}
		}else{
			MySqlUtil.SQLMANAGER
					.execute(new SQLReady("select b.novelid,b.name from bbs.pre_pdnovel_view b order by b.novelid"),
							Map.class)
					.forEach(book -> {
						JSONObject item = new JSONObject();
						item.put("novelid", book.get("novelid"));
						item.put("name", book.get("name"));
						books.add(item);
					});
		}
		map.put("books", books);
		HandlerUtil.setResp(exchange, map);
	}
	
	public void volumes(HttpServerExchange exchange) throws Exception {
		Integer novelid = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "novelid"), null);
		if(novelid != null) {
			Map<String, Object> map = new HashMap<>(1);
			JSONArray volumes = new JSONArray();
			if(loadFromFile){
				Book book = PdnovelUtil.books.get(novelid);
				if(book != null) {
					for(Entry<Integer, Volume> entry : book.volumes.entrySet()) {
						Integer volumeid = entry.getKey();
						Volume volume = entry.getValue();
						JSONObject item = new JSONObject();
						item.put("volumeid", volumeid);
						item.put("volumeorder", volume.volumeorder);
						item.put("volumename", volume.volumename);
						volumes.add(item);
					}
				}
			}else{
				MySqlUtil.SQLMANAGER
						.execute(new SQLReady(
								"SELECT v.novelid,v.volumeid,v.volumename,v.volumeorder FROM bbs.pre_pdnovel_volume v where v.novelid=? order by v.volumeid",
								novelid),
								Map.class)
						.forEach(volume -> {
							JSONObject item = new JSONObject();
							item.put("volumeid", volume.get("volumeid"));
							item.put("volumeorder", volume.get("volumeorder"));
							item.put("volumename", volume.get("volumename"));
							volumes.add(item);
						});
			}
			map.put("volumes", volumes);
			HandlerUtil.setResp(exchange, map);
		}
	}
	
	public void chapters(HttpServerExchange exchange) throws Exception {
		Integer novelid = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "novelid"), null);
		Integer volumeid = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "volumeid"), null);
		if(novelid!=null && volumeid!=null) {
			Map<String, Object> map = new HashMap<>(1);
			JSONArray chapters = new JSONArray();
			if(loadFromFile){
				Book book = PdnovelUtil.books.get(novelid);
				if(book != null) {
					Volume volume = book.volumes.get(volumeid);
					if(volume != null) {
						for(Entry<Integer, Chapter> entry : volume.chapters.entrySet()) {
							Chapter chapter = entry.getValue();
							JSONObject item = new JSONObject();
							item.put("chapterorder", chapter.chapterorder);
							item.put("chaptername", chapter.chaptername);
							item.put("chapterurl", UploadUtil.URL+"pdnovel/chapter/"+novelid+"/"+volumeid+"/"+chapter.chapterorder+".txt");
							chapters.add(item);
						}
					}
				}
			}else{
				MySqlUtil.SQLMANAGER
						.execute(new SQLReady(
								"SELECT c.chapterorder,c.chaptername FROM bbs.pre_pdnovel_chapter c where c.novelid=? and c.volumeid=? order by c.chapterorder",
								novelid, volumeid),
								Map.class)
						.forEach(chapter -> {
							JSONObject item = new JSONObject();
							item.put("chapterorder", chapter.get("chapterorder"));
							item.put("chaptername", chapter.get("chaptername"));
							item.put("chapterurl", UploadUtil.URL + "pdnovel/chapter/" + novelid + "/" + volumeid + "/"
									+ chapter.get("chapterorder") + ".txt");
							chapters.add(item);
						});
			}
			map.put("chapters", chapters);
			HandlerUtil.setResp(exchange, map);
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
