package com.xlongwei.light4j.util;

import org.apache.commons.codec.binary.Base64;

/**
 * 图片工具
 * @author xlongwei
 *
 */
public class ImageUtil {
	/** 判断是否base64字符串 */
	public static boolean isBase64(String base64) {
		return !StringUtil.isBlank(base64) && base64.indexOf("base64,") > -1;
	}

	/** 根据format添加base64前缀 */
	public static String prefix(String format) {
		return "data:" + ("WAV".equalsIgnoreCase(format) ? "audio" : "image") + "/"
				+ (StringUtil.isBlank(format) ? "PNG" : format) + ";base64,";
	}

	/** 从base64字符串获取format */
	public static String prefixFormat(String base64) {
		return StringUtil.getPatternString(base64, "data:(?:image|audio)/(\\p{Alnum}+);base64,");
	}

	/** 删除base64前缀 */
	public static String prefixRemove(String base64) {
		int tag = base64.indexOf("base64,");
		return tag > 0 ? base64.substring(tag + 7) : base64;
	}

	/**
	 * 编码图片或音频为base64字符串
	 * @param format PNG JPEG WAV
	 */
	public static String encode(byte[] img, String format) {
		return prefix(format) + Base64.encodeBase64String(img);
	}
}
