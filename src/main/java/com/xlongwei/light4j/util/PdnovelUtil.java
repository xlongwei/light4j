package com.xlongwei.light4j.util;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.xlongwei.light4j.util.FileUtil.CharsetNames;
import com.xlongwei.light4j.util.FileUtil.TextWriter;

import lombok.extern.slf4j.Slf4j;

/**
 * pdnovel util
 * 
 * @author xlongwei
 *
 */
@Slf4j
public class PdnovelUtil {
	public static final Map<Integer, Book> books = new LinkedHashMap<>();
	private static final Pattern chapterNamePattern = Pattern.compile("[^']+'([^',]*)','([^',]*)','([^',]*)','([^',]*)','([^',]*)','([^',]*)','([^',]*)','([^',]*)','([^',]*)','([^',]*)','([^',]*)','([^',]*)'\\);");

	public static void reload() {
		String pdnovel = UploadUtil.SAVE + "pdnovel";
		log.info("pdnovel dir: {}", pdnovel);
		List<String> lines = FileUtil.readLines(new File(pdnovel, "books.txt"), CharsetNames.UTF_8);
		log.info("pdnovel books: {}", lines.size());
		books.clear();
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
	
	/**
	 * 合并书卷文本
	 * @param novelid 0全部或指定书
	 * @param volumeid 0全部或指定卷
	 */
	public static void merge(int novelid, int volumeid) {
		String uploadsFile = UploadUtil.SAVE;
		File novels = new File(StringUtil.firstNotBlank(RedisConfig.get("pdnovel.dir"), "/soft/discuz/data/attachment/pdnovel/chapter"));
		log.info("pdnovel merge novelid: {}, volumeid: {}, novels: {}, exists: {}", novelid, volumeid, novels.getAbsoluteFile(), novels.exists());
		File[] novelFiles = novels.exists() ? novels.listFiles() : new File[0];
		Arrays.sort(novelFiles, FileUtil.fileComparator);
		for(File novelFile : novelFiles) {
			int novelid2 = NumberUtil.parseInt(novelFile.getName(), 0);
			if(!novelFile.isDirectory() || novelid2<=0) {
				continue;
			}
			if(novelid>0 && novelid!=novelid2) {
				continue;//指定novelid时覆盖处理，不指定时披露处理但不覆盖
			}
			boolean novelForce = novelid>0&&novelid==novelid2;
			File novelTarget = new File(uploadsFile, "pdnovel/"+novelid2+".txt");
			if(novelTarget.exists() && !novelForce) {
				continue;
			}
			File[] volumeFiles = novelFile.listFiles();
			Arrays.sort(volumeFiles, FileUtil.fileComparator);
			TextWriter novelWriter = new TextWriter(novelTarget, CharsetNames.UTF_8, false, true);
			int volumeIndex = 1;
			for(File volumeFile : volumeFiles) {
				int volumeid2 = NumberUtil.parseInt(volumeFile.getName(), 0);
				if(!volumeFile.isDirectory() || volumeid2<=0) {
					continue;
				}
				File volumeSql = new File(volumeFile, volumeFile.getName()+".sql");
				String volumeName = ""; Map<String, String> chapterNames = new HashMap<>(16);
				if(volumeSql.exists()) {
					List<String> lines = FileUtil.readLines(volumeSql, CharsetNames.UTF_8);
					for(String line:lines) {
						Matcher matcher = chapterNamePattern.matcher(line);
						if(matcher.matches()) {
							chapterNames.put(matcher.group(9), matcher.group(8));
						} else {
							volumeName = StringUtil.getPatternString(line, "[^']+'[^',]*','[^',]*','([^',]*)','[^',]*','[^',]*','[^',]*'\\);");
						}
					}
				}
				File volumeTarget = new File(uploadsFile, "pdnovel/"+novelid2+"/"+volumeid2+".txt");
				if(!novelForce && volumeid>0 && volumeid!=volumeid2 && volumeTarget.exists()) {
					novelWriter.writeln("第"+(volumeIndex++)+"卷 "+volumeName);
					novelWriter.writeln(FileUtil.readString(volumeTarget, CharsetNames.UTF_8));
					continue;
				}
				boolean volumeForce = novelForce || (volumeid>0&&volumeid==volumeid2);
				if(volumeTarget.exists() && !volumeForce) {
					continue;
				}
				if(!volumeTarget.exists()) {
					volumeTarget.getParentFile().mkdirs();
				}
				File[] chapterFiles = volumeFile.listFiles(new FileUtil.ExtFileFilter("txt"));
				Arrays.sort(chapterFiles, FileUtil.fileComparator);
				TextWriter volumeWriter = new TextWriter(volumeTarget, CharsetNames.UTF_8, false, true);
				for(File chapterFile : chapterFiles) {
					String chapterName = chapterNames.get(FileUtil.getFileName(chapterFile.getName()));
					volumeWriter.writeln("第"+FileUtil.getFileName(chapterFile)+"章 "+(chapterName!=null?chapterName:""));
					volumeWriter.writeln(FileUtil.readString(chapterFile, CharsetNames.UTF_8));
				}
				volumeWriter.close();
				log.info("pdnovel merge success, volumeid: "+volumeid2+", volumeIndex: "+volumeIndex);
				novelWriter.writeln("第"+(volumeIndex++)+"卷");
				novelWriter.writeln(FileUtil.readString(volumeTarget, CharsetNames.UTF_8));
			}
			novelWriter.close();
			log.info("pdnovel merge success, novelid: "+novelid2);
		}
		log.info("pdnovel merge finish");
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
