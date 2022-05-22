package com.xlongwei.light4j.util;

import java.io.File;
import java.util.Comparator;

import org.apache.commons.lang3.reflect.FieldUtils;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.pinyin4j.PinyinHelper2;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;

/**
 * @author Hongwei
 */
@Slf4j
public class PinyinUtil {
	/**
	 * return pinyin of zhStr, eg: zhongguo for 中国
	 */
	public static String getPinyin(String zhStr) {
		if(StringUtil.isBlank(zhStr)) {
			return "";
		}
		String[] pinyin = getPinyin(zhStr, 0, 1, 0);
		StringBuilder sb = new StringBuilder();
		for(String str : pinyin) {
			sb.append(str);
		}
		return sb.toString();
	}

	/**
	 * return pinyin of zhCh, eg: zhong for 中
	 */
	public static String getPinyin(char zhCh) {
		String zhChStr = Character.toString(zhCh);
		if (StringUtil.isChinese(zhCh)) {
			try {
				return PinyinHelper2.toHanYuPinyinString(zhChStr, format)[0];
			} catch (Exception e) {
			}
		}
		return zhChStr;
	}
	
	/**
	 * 处理句子
	 * @param caseType 0-lower 1-camel 2-upper
	 * @param toneType 0-mark 1-no 2-number
	 * @param vcharType 0-ü 1-v 2-u: （toneType=0时必须vcharType=0）
	 */
	public static String[] getPinyin(String sentence, int caseType, int toneType, int vcharType) {
		if(toneType!=1 && toneType!=2) vcharType=0; //avoid BadHanyuPinyinOutputFormatCombination
		try{
			HanyuPinyinOutputFormat outputFormat = new HanyuPinyinOutputFormat();
			outputFormat.setToneType(toneType==1?HanyuPinyinToneType.WITHOUT_TONE:(toneType==2?HanyuPinyinToneType.WITH_TONE_NUMBER:HanyuPinyinToneType.WITH_TONE_MARK));
			outputFormat.setVCharType(vcharType==1?HanyuPinyinVCharType.WITH_V:(vcharType==2?HanyuPinyinVCharType.WITH_U_AND_COLON:HanyuPinyinVCharType.WITH_U_UNICODE));
			String[] split = PinyinHelper2.toHanYuPinyinString(sentence, outputFormat);
			if(caseType==1 || caseType==2) {
				for(int i=0,len=split.length;i<len;i++){
					if(caseType==1) {
						split[i] = split[i].toUpperCase();
					} else if(caseType==2) {
						split[i] = StringUtil.capitalize(split[i]);
					}
				}
			}
			return split;
		}catch(Exception e) {
			return sentence.split("");
		}
	}

	/**
	 * 中文拼音字符串比较器
	 */
	public static final Comparator<String> ZH_COMPARATOR = (o1, o2) -> {
			if (o1 == null) {
				if (o2 == null) {
					// 空 = 空
					return 0;
				}
				else {
					// 空 < 非空
					return -1; 
				}
			} else {
				if (o2 == null) {
					// 非空 > 空
					return 1;
				} else {
					return getPinyin(o1).compareTo(getPinyin(o2));
				}
			}
	};
	
	/**
	 * 获取实体某个中文字段的比较器
	 */
	public static <Entity> Comparator<Entity> zhStringFieldComparator(Class<Entity> clazz, final String zhStringField){
		return (o1, o2) -> {
				try{
					String left = (String)FieldUtils.readField(o1, zhStringField, true);
					String right = (String)FieldUtils.readField(o2, zhStringField, true);
					return ZH_COMPARATOR.compare(left, right);
				}catch(Exception e) {
					log.warn("fail to zhCompare, ex={}, msg={}", e.getClass().getSimpleName(), e.getMessage());
				}
				return 0;
		};
	}
	
	/** 文件名是拼音时，按拼音顺序排序 */
	public static class PinyinFileNameComparator implements Comparator<File> {
		@Override
		public int compare(File o1, File o2) {
			String left = FileUtil.getFileName(o1);
			String right = FileUtil.getFileName(o1);
			return ZH_COMPARATOR.compare(left, right);
		}
	}
	
	public static HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
	static {
		format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
		format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
		format.setVCharType(HanyuPinyinVCharType.WITH_V);
	}
}
