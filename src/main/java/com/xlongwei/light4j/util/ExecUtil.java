package com.xlongwei.light4j.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DaemonExecutor;
import org.apache.commons.exec.ExecuteStreamHandler;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.OS;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FilenameUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 使用find+grep检索日志
 * @author xlongwei
 *
 */
@Slf4j
public class ExecUtil {
	public static boolean isWindows = OS.isFamilyWindows();
	
	/**
	 * 搜索dir目录下的日志文件，列出包含search文本的文件名
	 * @param dir
	 * @param search
	 * @return
	 */
	public static List<String> list(String dir, String search) {
		try{
			String find = find(dir, search);
			String[] lines = find.split("[\r\n]+");
			List<String> files = new ArrayList<>();
			for(String line : lines) {
				if(StringUtil.isBlank(line)) {
					continue;
				}else if(isWindows) {
					int e = line.indexOf(':'), s = e==-1 ? -1 : line.lastIndexOf(' ', e);
					if(s == -1) {
						continue;
					}else if(NumberUtil.parseInt(line.substring(e+1).trim(), 0) > 0) {
						files.add(line.substring(s, e).trim());
					}
				}else {
					files.add(FilenameUtils.getName(line));
				}
			}
			return files;
		}catch(Exception e) {
			log.info("fail to find dir:{}+search:{}, ex:{}", dir, search, e.getMessage());
		}
		return Collections.emptyList();
	}
	
	public static String find(String dir, String search) throws Exception {
		CommandLine command = null;
		if(isWindows) {
			//find /C "a" *
			command = CommandLine.parse("find");
			command.addArgument("/C");
			command.addArgument("\"${search}\"", false);
			command.addArgument("*");
		}else {
			//find . -type f -exec zgrep -li '${search}' {} \;
			//find ! -name *.gz -exec grep -li '${search}' {} \;
			//1，命令行的分号需要转义\;或引号';'而CommandLine不需要；2，为提高效率可以自定义查找最近几天内的日志，这里查找未gz压缩的日志（通过gz规则控制数量）
			//command = CommandLine.parse("find . -type f -exec zgrep -li ''${search}'' {} ;");
			command = CommandLine.parse("find ! -name *.gz -exec grep -li ''${search}'' {} ;");
		}
		Map<String, Object> substitutionMap = new HashMap<>(4);
		substitutionMap.put("search", search);
		command.setSubstitutionMap(substitutionMap);
		
		Executor exe = new DaemonExecutor();
		exe.setWorkingDirectory(new File(dir));
		exe.setExitValues(new int[]{0,1,2});
		
		ExecuteWatchdog watchDog = new ExecuteWatchdog(60000);
		exe.setWatchdog(watchDog);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ExecuteStreamHandler streamHandler = new PumpStreamHandler(baos);
		exe.setStreamHandler(streamHandler);
		
		exe.execute(command);
		String out = baos.toString(isWindows ? "GBK" : "UTF-8");
		log.debug(out);
		return out;
	}
}
