package com.xlongwei.light4j.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;

import com.xlongwei.light4j.beetl.model.Ecdict;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.CharUtil;

/**
 * ecdict util
 * @author xlongwei
 *
 */
public class EcdictUtil {
	/** 将string分割为中文、英文句子 */
	public static List<String> sentences(String sentence) {
		List<String> sentences = new ArrayList<>();
		int cur = 0, len = sentence.length();
		next: for(;cur < len;) {
			char c = sentence.charAt(cur);
			boolean zh = StringUtil.isChinese(c), en = CharUtil.isLetter(c);
			for(int pos = cur+1; pos < len; pos++) {
				c = sentence.charAt(pos);
				boolean z = StringUtil.isChinese(c), e = CharUtil.isLetter(c);
				boolean change = (!zh && !en && (z || e)) //非中非英=》中或英
						|| (zh != z && en != e);//中或英=》英或中
				if(change) {
					String next = sentence.substring(cur, pos);
					sentences.add(next);
					cur = pos;
					continue next;
				}
			}
			String next = sentence.substring(cur);
			sentences.add(next);
			break;
		}
		return sentences;
	}
	/** 获取英文句子的音标 */
	public static List<String> words(String sentence) {
		List<String> words = new ArrayList<>();
		int cur = 0, len = sentence.length();
		next: for(;cur < len;) {
			char c = sentence.charAt(cur);
			boolean en = CharUtil.isLetter(c);
			for(int pos = cur+1; pos < len; pos++) {
				c = sentence.charAt(pos);
				boolean e = CharUtil.isLetter(c) || c=='\'';//won't
				boolean change = en != e;
				if(change) {
					String word = sentence.substring(cur, pos);
					if(!StringUtil.isBlank(word)) {
						words.add(word);
					}
					cur = pos;
					continue next;
				}
			}
			String word = sentence.substring(cur);
			if(!StringUtil.isBlank(word)) {
				words.add(word);
			}
			break;
		}
		return words;
	}
	/** 获取英文句子的音标 */
	public static List<String> pinyin(List<String> words) {
		List<String> pinyin = new ArrayList<>(words);
		Map<String, String> phonetic = phonetic(words.stream().map(String::toLowerCase).filter(EcdictUtil::isWord).collect(Collectors.toSet()));
		for(int cur = 0, len = words.size(); cur < len; cur++) {
			String word = words.get(cur).toLowerCase();
			if(isWord(word)) {
				String symbol = phonetic.get(word);
				if(!StringUtil.isBlank(symbol)) {
					pinyin.set(cur, symbol);
				}
			}
		}
		return pinyin;
	}
	/** 获取多个单词的音标 */
	public static Map<String, String> phonetic(Set<String> words) {
		if(CollUtil.isEmpty(words)) {
			return Collections.emptyMap();
		}
		int pageSize = 100;
		if(words.size() <= pageSize) {
			List<Ecdict> list = MySqlUtil.SQLMANAGER.lambdaQuery(Ecdict.class).andIn("word", words).select("word","phonetic");
			return list.stream().collect(Collectors.toMap(ec -> ec.getWord(), ec -> ec.getPhonetic(), (v1,v2) -> v1));
		}else {
			List<List<String>> partitions = ListUtils.partition(new ArrayList<>(words), pageSize);
			Map<String, String> phonetic = new HashMap<>(words.size());
			for(List<String> partition : partitions) {
				List<Ecdict> list = MySqlUtil.SQLMANAGER.lambdaQuery(Ecdict.class).andIn("word", partition).select("word","phonetic");
				Map<String, String> map = list.stream().collect(Collectors.toMap(ec -> ec.getWord(), ec -> ec.getPhonetic(), (v1,v2) -> v1));
				phonetic.putAll(map);
			}
			return phonetic;
		}
	}
	/** 判断英文单词是否合法 */
	public static boolean isWord(String word) {
		if(word==null || word.length()==0) {
			return false;
		}
		int len = word.length();
		//首尾必须是字母，中间可以有特殊字符：won't 
		char s = word.charAt(0), e = word.charAt(len-1);
		if(CharUtil.isLetter(s) && CharUtil.isLetter(e)) {
			for(int i=1,j=len-1;i<j;i++) {
				char c = word.charAt(i);
				if(CharUtil.isLetter(c) || c=='\'' || c=='-') {
					continue;
				}else {
					return false;
				}
			}
			return true;
		}
		return false;
	}
}
