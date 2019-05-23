package com.xlongwei.light4j.util;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.nlpcn.commons.lang.util.StringUtil;

import com.xlongwei.light4j.util.FileUtil.CharsetNames;

import lombok.extern.slf4j.Slf4j;

/**
 * pdnovel util
 * 
 * @author xlongwei
 *
 */
@Slf4j
public class PdnovelUtil {
	public static Map<Integer, Book> books = new LinkedHashMap<>();

	static {
		String pdnovel = UploadUtil.SAVE + "pdnovel";
		log.info("pdnovel dir: {}", pdnovel);
		List<String> lines = FileUtil.readLines(new File(pdnovel, "books.txt"), CharsetNames.UTF_8);
		log.info("pdnovel books: {}", lines.size());
		for (String line : lines) {
			if (StringUtil.isBlank(line)) {
				continue;
			}
			// novelid,name
			String[] split = line.split("[,]");
			int idx = 0, novelid = Integer.parseInt(split[idx++]);
			Book book = new Book();
			books.put(novelid, book);
			book.name = split[idx++];
			book.volumes = new LinkedHashMap<>();
		}
		lines = FileUtil.readLines(new File(pdnovel, "volumes.txt"), CharsetNames.UTF_8);
		log.info("pdnovel volumes: {}", lines.size());
		for (String line : lines) {
			if (StringUtil.isBlank(line)) {
				continue;
			}
			// novelid,volumeid,volumeorder,volumename
			String[] split = line.split("[,]");
			int idx = 0, novelid = Integer.parseInt(split[idx++]);
			int volumeid = Integer.parseInt(split[idx++]);
			Book book = books.get(novelid);
			Volume volume = new Volume();
			book.volumes.put(volumeid, volume);
			volume.volumeorder = Integer.parseInt(split[idx++]);
			volume.volumename = split.length > idx ? split[idx++] : book.name;
			volume.chapters = new LinkedHashMap<>();
		}
		lines = FileUtil.readLines(new File(pdnovel, "chapters.txt"), CharsetNames.UTF_8);
		log.info("pdnovel chapters: {}", lines.size());
		for (String line : lines) {
			if (StringUtil.isBlank(line)) {
				continue;
			}
			// novelid,volumeid,chapterid,chapterorder,chaptername
			String[] split = line.split("[,]");
			int idx = 0, novelid = Integer.parseInt(split[idx++]);
			int volumeid = Integer.parseInt(split[idx++]);
			int chapterid = Integer.parseInt(split[idx++]);
			Volume volume = books.get(novelid).volumes.get(volumeid);
			Chapter chapter = new Chapter();
			volume.chapters.put(chapterid, chapter);
			chapter.chapterorder = Integer.parseInt(split[idx++]);
			chapter.chaptername = split.length > idx ? split[idx++] : volume.volumename;
		}
		log.info("pdnovel init success");
	}

	public static class Book {
		public String name;
		public Map<Integer, Volume> volumes;
	}

	public static class Volume {
		public int volumeorder;
		public String volumename;
		public Map<Integer, Chapter> chapters;
	}

	public static class Chapter {
		public int chapterorder;
		public String chaptername;
	}
}
