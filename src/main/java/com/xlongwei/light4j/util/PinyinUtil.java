package com.xlongwei.light4j.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xlongwei.light4j.util.FileUtil.TextReader;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

/**
 * @author Hongwei
 */
public class PinyinUtil {
	/**
	 * return pinyin of zhStr, eg: zhongguo for 中国
	 */
	public static String getPinyin(String zhStr) {
		StringBuilder pinyin = new StringBuilder();
		char[] zhCharArray = zhStr.toCharArray();
		for (char zhChar : zhCharArray) {
			pinyin.append(getPinyin(zhChar));
		}
		return pinyin.toString();
	}

	/**
	 * return pinyin of zhCh, eg: zhong for 中
	 */
	public static String getPinyin(char zhCh) {
		String zhChStr = Character.toString(zhCh);
		if (isChineseCharacter(zhCh)) {
			try {
				return PinyinHelper.toHanyuPinyinStringArray(zhCh, format)[0];
			} catch (Exception e) {
			}
		}
		return zhChStr;
	}

	/**
	 * return pinyin header of zhStr, eg: zg for 中国
	 */
	public static String getPinyinHeader(String zhStr) {
		StringBuilder pinyin = new StringBuilder();
		char[] zhCharArray = zhStr.toCharArray();
		for (char zhChar : zhCharArray) {
			String str = getPinyin(zhChar);
			if (str.length() > 0)
				pinyin.append(str.charAt(0));
		}
		return pinyin.toString();
	}

	/**
	 * return true if Char c is chinese character.
	 */
	public static boolean isChineseCharacter(char c) {
		// return String.valueOf(c).matches("[\\u4E00-\\u9FA5]+");
		Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
		if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
				|| ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
				|| ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
				|| ub == Character.UnicodeBlock.GENERAL_PUNCTUATION
				|| ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
				|| ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS) {
			return true;
		}
		return false;
	}
	
	/** 处理单子拼音，多音字有多个 */
	public static String[] getPinyin(char ch, HanyuPinyinToneType toneType, HanyuPinyinVCharType vcharType) {
		try {
			if(ch=='\u3007' || (ch>='\u4e00'&&ch<='\u9fa5')) {
				HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
				format.setToneType(toneType!=null ? toneType : HanyuPinyinToneType.WITHOUT_TONE);
				format.setVCharType(vcharType!=null ? vcharType : HanyuPinyinVCharType.WITH_V);
				String[] pinyin = PinyinHelper.toHanyuPinyinStringArray(ch, format);
				if(pinyin!=null && pinyin.length>0) return pinyin;
			}
		} catch (BadHanyuPinyinOutputFormatCombination e) {}
		return new String[] {String.valueOf(ch)};
	}
	
	/**
	 * 处理多音字
	 * @param caseType 0-lower 1-camel 2-upper
	 * @param toneType 0-mark 1-no 2-number
	 * @param vcharType 0-ü 1-v 2-u: （toneType=0时必须vcharType=0）
	 */
	public static String[] getPinyin(char ch, int caseType, int toneType, int vcharType) {
		if(toneType!=1 && toneType!=2) vcharType=0;
		String[] pinyin = getPinyin(ch, toneType==1?HanyuPinyinToneType.WITHOUT_TONE:(toneType==2?HanyuPinyinToneType.WITH_TONE_NUMBER:HanyuPinyinToneType.WITH_TONE_MARK), vcharType==1?HanyuPinyinVCharType.WITH_V:(vcharType==2?HanyuPinyinVCharType.WITH_U_AND_COLON:HanyuPinyinVCharType.WITH_U_UNICODE));
		if(caseType==1 || caseType==2)
		for(int i=0; i<pinyin.length; i++) {
			if(caseType==1) pinyin[i] = pinyin[i];
			else pinyin[i] = pinyin[i].toUpperCase();
		}
		return pinyin;
	}
	
	/** 处理多音字，根据上下文选择正确的拼音 */
	public static String getPinyin(String sentence, int index, HanyuPinyinToneType toneType, HanyuPinyinVCharType vcharType) {
		String[] pinyin = getPinyin(sentence.charAt(index), toneType, vcharType);
		if(pinyin.length>1) {//多音字处理
			String[] pinyins = ((toneType==null||toneType==HanyuPinyinToneType.WITHOUT_TONE)&&(vcharType==null||vcharType==HanyuPinyinVCharType.WITH_V)) ? pinyin : getPinyin(sentence.charAt(index), HanyuPinyinToneType.WITHOUT_TONE, HanyuPinyinVCharType.WITH_V);
			List<String> words = new ArrayList<>();
			int left = Math.max(index - duoyinziMax + 1, 0), len = sentence.length();
			for(int i=left; i<=index; i++) {
				for(int j=Math.max(index, i+1); j<Math.min(i+duoyinziMax,len); j++) {
					words.add(sentence.substring(i, j+1));
				}
			}
			words.add(sentence.substring(index, index+1));//更改默认读音
			for(String word : words) {
				String py = duoyinzi.get(word);
				if(py!=null) {
					int i = ArrayUtils.indexOf(pinyins, py);
					if(i>-1) return pinyin[i];
				}
			}
		}
		return pinyin[0];
	}
	
	/**
	 * 处理句子
	 * @param caseType 0-lower 1-camel 2-upper
	 * @param toneType 0-mark 1-no 2-number
	 * @param vcharType 0-ü 1-v 2-u: （toneType=0时必须vcharType=0）
	 */
	public static String[] getPinyin(String sentence, int caseType, int toneType, int vcharType) {
		if(toneType!=1 && toneType!=2) vcharType=0; //avoid BadHanyuPinyinOutputFormatCombination
		String[] pinyin = new String[sentence.length()];
		for(int i=0,len=pinyin.length; i<len; i++) {
			String py = getPinyin(sentence, i, toneType==1?HanyuPinyinToneType.WITHOUT_TONE:(toneType==2?HanyuPinyinToneType.WITH_TONE_NUMBER:HanyuPinyinToneType.WITH_TONE_MARK)
					, vcharType==1?HanyuPinyinVCharType.WITH_V:(vcharType==2?HanyuPinyinVCharType.WITH_U_AND_COLON:HanyuPinyinVCharType.WITH_U_UNICODE));
			if(caseType==1) pinyin[i] = StringUtil.capitalize(py);
			else if(caseType==2) pinyin[i] = py.toUpperCase();
			else pinyin[i] = py;
		}
		return pinyin;
	}

	/**
	 * 中文拼音字符串比较器
	 */
	public static final Comparator<String> zhComparator = new Comparator<String>() {
		@Override
		public int compare(String o1, String o2) {
			if (o1 == null) {
				if (o2 == null) return 0; // 空 = 空
				else return -1; // 空 < 非空
			} else {
				if (o2 == null) return 1; // 非空 > 空
				else {
					int min = Math.min(o1.length(), o2.length());// 比较前min个字符
					for (int i = 0; i < min; i++) {
						char c1 = o1.charAt(i), c2 = o2.charAt(i);
						if (isChineseCharacter(c1)) {
							if (!isChineseCharacter(c2)) return 1; // 中文 > 英文
							else if (c1 != c2) return getPinyin(c1).compareTo(getPinyin(c2)); // 两个中文比较
						} else {
							if (isChineseCharacter(c2)) return -1; // 英文 < 中文
							else if (c1 != c2) return String.valueOf(c1).compareToIgnoreCase(String.valueOf(false)); // 两个英文比较
						}
					}
					return o1.length() < min ? -1 : (o2.length() < min ? 1 : 0);// 短串 < 长串
				}
			}// else
		}// if
	};
	
	/**
	 * 获取实体某个中文字段的比较器
	 */
	public static <Entity> Comparator<Entity> zhStringFieldComparator(Class<Entity> clazz, final String zhStringField){
		return new Comparator<Entity>() {
			@Override
			public int compare(Entity o1, Entity o2) {
				try{
					String left = (String)FieldUtils.readField(o1, zhStringField, true);
					String right = (String)FieldUtils.readField(o2, zhStringField, true);
					return zhComparator.compare(left, right);
				}catch(Exception e) {
					e.printStackTrace();
				}
				return 0;
			}
		};
	}
	
	/** 文件名是拼音时，按拼音顺序排序 */
	public static class PinyinFileNameComparator implements Comparator<File> {
		@Override
		public int compare(File o1, File o2) {
			String left = FileUtil.getFileName(o1);
			String right = FileUtil.getFileName(o1);
			return zhComparator.compare(left, right);
		}
	}
	
	private static final HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
	private static final Logger logger = LoggerFactory.getLogger(PinyinUtil.class);
	public static final Map<String, String> duoyinzi = new HashMap<>();
	private static int duoyinziMax = 1;
	static {
		format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
		format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
		format.setVCharType(HanyuPinyinVCharType.WITH_V);
		
		try {
			TextReader reader = new TextReader(ConfigUtil.stream("duoyinzi.txt"), CharEncoding.UTF_8);
			String line = null;
			while(StringUtils.isNotBlank(line=reader.read())) {
				String[] split = line.split("#");
				String[] words = split[1].split("\\s+");
				for(String word:words) {
					duoyinzi.put(word, split[0]);
					if(word.length()>duoyinziMax) duoyinziMax = word.length();
				}
			}
			reader.close();
			logger.info("duoyinzi words: "+duoyinzi.size()+", max length: "+duoyinziMax);
		} catch (Exception e) {
			logger.warn("fail to load duoyinzi.txt: "+e.getMessage());
		}
	}
}
