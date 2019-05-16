package com.xlongwei.light4j.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.codec.binary.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 文件上传工具类
 * @author xlongwei
 *
 */
@Slf4j
public class UploadUtil {
	public static final Map<String, String> CONFIG = ConfigUtil.config("upload");
	/** SAVE-文件系统保存目录 URL-互联网访问网址 */
	public static final String SAVE = CONFIG.get("save"), URL = CONFIG.get("url");
	/** SAVE-临时文件保存目录 URL-临时文件访问网址 */
	public static final String SAVE_TEMP = SAVE + "temp/", URL_TEMP = URL + "temp/";
	/** 文件上传操作码 */
	public static final String DIRECT = "direct", TEMP = "temp", CONFIRM = "confirm", TRASH = "trash";
	/** 文件上传响应码 */
	public static final String DOMAIN ="domain", PATH = "path";
	
	/**
	 * 根据文件名获取可用保存路径
	 * @param type image word pdf等
	 * @param fileName
	 * @return
	 */
	public static String path(String type, String fileName) {
		type = StringUtil.isBlank(type) ? "" : type.trim()+"/";
		String name = FileUtil.getFileName(fileName), ext = FileUtil.getFileExt(fileName);
		if(StringUtil.isHasChinese(fileName)) {
			fileName = name + "." + IdWorker.getId() + (StringUtil.isBlank(ext) ? "" : "." + ext);
		}
		String dir = SAVE_TEMP, file = type+fileName;
		File target = new File(dir, file);
		while(target.exists()) {
			file = type + name + "." + IdWorker.getId() + (StringUtil.isBlank(ext) ? "" : "." + ext);
			target = new File(dir, file);
		}
		return file;
	}
	
	/** 尝试处理中文乱码 */
	public static String string(String string) {
		return StringUtils.newStringIso8859_1(StringUtils.getBytesUtf8(string));
	}
	
	/**
	 * 保存文件
	 * @param is
	 * @param target
	 * @return
	 */
	public static boolean save(InputStream is, File target) {
		if(target.getParentFile().exists()==false) {
			target.getParentFile().mkdirs();
		}
		try {
			FileOutputStream out = new FileOutputStream(target, false);
			FileUtil.copyStream(is, out);
			out.close();
			log.info("save file to target:{}", target);
			return true;
		} catch (IOException e) {
			log.warn("fail to save file to target:{}", target);
		}
		return false;
	}
	
	/**
	 * 移动文件
	 * @param path path/name.ext
	 * @param type confirm：temp->uploads，trash：uploads->trash
	 * @return
	 */
	public static boolean move(String path, String type) {
		if(StringUtil.isBlank(path)) {
			return false;
		}
		String from = null, to = null;
		if(CONFIRM.equals(type)) {
			from = SAVE_TEMP;
			to = SAVE;
		}else if(TRASH.equals(type)) {
			from = SAVE;
			to = SAVE + "/trash";
		}else {
			return false;
		}
		File source = new File(from, path);
		if(!source.exists() || source.isFile()==false) {
			log.info("move "+path+" from: "+from+" to: " + to + ", source not exist or not file.");
			return false;
		}
		File target = new File(to, path), targetParent = target.getParentFile();
		if(targetParent.exists()==false) {
			targetParent.mkdirs();
		}
		boolean b = FileUtil.tryRenameTo(source, target, 3);
		log.info("move:{} file:{} from:{} to:{}", b, path,from, to);
		return b;
	}
}
