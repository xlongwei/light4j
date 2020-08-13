package com.xlongwei.light4j.util;
//
//import java.io.InputStream;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
//
//import org.ansj.app.keyword.KeyWordComputer;
//import org.ansj.app.keyword.Keyword;
//import org.ansj.app.summary.SummaryComputer;
//import org.ansj.app.summary.TagContent;
//import org.ansj.app.summary.pojo.Summary;
//import org.ansj.domain.Result;
//import org.ansj.domain.Term;
//import org.ansj.library.AmbiguityLibrary;
//import org.ansj.library.DicLibrary;
//import org.ansj.library.StopLibrary;
//import org.ansj.library.SynonymsLibrary;
//import org.ansj.splitWord.Analysis;
//import org.ansj.splitWord.analysis.BaseAnalysis;
//import org.ansj.splitWord.analysis.DicAnalysis;
//import org.ansj.splitWord.analysis.IndexAnalysis;
//import org.ansj.splitWord.analysis.ToAnalysis;
//import org.ansj.util.MyStaticValue;
//import org.nlpcn.commons.lang.jianfan.JianFan;
//import org.nlpcn.commons.lang.pinyin.CaseType;
//import org.nlpcn.commons.lang.pinyin.Pinyin;
//import org.nlpcn.commons.lang.pinyin.PinyinFormat;
//import org.nlpcn.commons.lang.pinyin.PinyinFormatter;
//import org.nlpcn.commons.lang.pinyin.ToneType;
//import org.nlpcn.commons.lang.pinyin.YuCharType;

//import com.xlongwei.light4j.util.AdtUtil.PairList;

/**
 * ansj_seg分词封装
 * @author xlongwei
 *
 */
public class FenciUtil {
//	public static final List<String> EMPTY_STRING_LIST = Collections.emptyList();
//	private static final InputStream STOPWORDS_STREAM = ConfigUtil.stream("stopwords.txt");
//	private static final Map<String, Analysis> METHODS = new HashMap<>();
//	public static final List<String> STOPWORDS_LIST = STOPWORDS_STREAM!=null ? FileUtil.readLines(STOPWORDS_STREAM, FileUtil.CharsetNames.UTF_8) : EMPTY_STRING_LIST;
//	static {
//		MyStaticValue.isRealName = Boolean.TRUE;
//		MyStaticValue.ENV.putIfAbsent(AmbiguityLibrary.DEFAULT, ConfigUtil.DIRECTORY + "ambiguity.dic");
//		MyStaticValue.ENV.putIfAbsent(StopLibrary.DEFAULT, ConfigUtil.DIRECTORY + "stop.dic");
//		if(!StringUtil.isUrl(ConfigUtil.DIRECTORY)) {
//			//文件较大，不从网络加载，仅支持从文件加载（crf.model文件已损坏）
//			//MyStaticValue.ENV.putIfAbsent(CrfLibrary.DEFAULT, ConfigUtil.directory + "crf.model");
//			MyStaticValue.ENV.putIfAbsent(DicLibrary.DEFAULT, ConfigUtil.DIRECTORY + "default.dic");
//			MyStaticValue.ENV.putIfAbsent(SynonymsLibrary.DEFAULT, ConfigUtil.DIRECTORY + "synonyms.dic");
//		}
//	}
//	public static enum Method {
//		/**
//		 * 分词方式
//		 */
//		BASE, DIC, INDEX, NLP, TO;
//		public static Method of(String method, Method defVal) {
//			if(!StringUtil.isBlank(method)) {
//				for(Method m : Method.values()) {
//					if(m.name().equalsIgnoreCase(method)) {
//						return m;
//					}
//				}
//			}
//			return defVal;
//		}
//	};
//	
//	public static Analysis getAnalysis(Method method) {
//		if(method == null) {
//			method = Method.TO;
//		}
//		Analysis a = METHODS.get(method.name());
//		if(a == null) {
//			switch(method) {
//			case TO: a = new ToAnalysis(); break;
////			case NLP: a = new NlpAnalysis(); break;
//			case DIC: a = new DicAnalysis(); break;
//			case INDEX: a = new IndexAnalysis(); break;
//			case BASE: a = new BaseAnalysis(); break;
//			default: a= new ToAnalysis(); break;
//			}
//			METHODS.put(method.name(), a);
//		}
//		return a;
//	}
//	
//	/** 标准分词
//	 * @param stopword true 去掉停止词
//	 */
//	public static List<String> segments(String text, boolean stopword) {
//		if(StringUtil.isBlank(text)) {
//			return EMPTY_STRING_LIST;
//		}
//		List<String> list = fenci(text, Method.TO);
//		if(!stopword || list.isEmpty()) {
//			return list;
//		}
//		List<String> result = new ArrayList<>();
//		for(String name : list) {
//			if(!STOPWORDS_LIST.contains(name)) {
//				result.add(name);
//			}
//		}
//		return result;
//	}
//
//	/** frequent 分词频次倒排 */
//	public static List<String> frequency(String content, int num) {
//		List<String> parse = segments(content, false);
//		Map<String, Integer> counts = new HashMap<>(8);
//		for(String word : parse) {
//			if(StringUtil.isBlank(word) || word.length()<2) {
//				continue;
//			}
//			Integer count = counts.get(word);
//			if(count==null) {
//				counts.put(word, 1);
//			} else {
//				counts.put(word, count+1);
//			}
//		}
//		PairList<String, Integer> pairs = new PairList.PriorityPairList<>();
//		for(String keyword : counts.keySet()) {
//			pairs.put(keyword, counts.get(keyword));
//		}
//		List<String> keywords = new LinkedList<>();
//		while(pairs.moveNext()) {
//			keywords.add(pairs.getData());
//		}
//		return num>0 ? keywords.subList(0, num) : keywords;
//	}
//	
//	/** ansj 提取关键字 */
//	public static List<String> keywords(String title, String content, int num, Method method){
//		if(StringUtil.isBlank(title) && StringUtil.isBlank(content)) {
//			return EMPTY_STRING_LIST;
//		}
//		if(method==null) {
//			method = Method.TO;
//		}
//		KeyWordComputer<?> kwc = new KeyWordComputer<Analysis>(num, getAnalysis(method));
//		Collection<Keyword> keywords = kwc.computeArticleTfidf(title, content);
//		if(keywords==null || keywords.size()==0) {
//			return EMPTY_STRING_LIST;
//		}
//		List<String> keys = new LinkedList<>();
//		for(Keyword keyword : keywords) {
//			keys.add(keyword.getName());
//		}
//		return keys;
//	}
//	
//	/** ansj 各种分词 */
//	public static List<String> fenci(String content, Method method) {
//		if(StringUtil.isBlank(content)) {
//			return EMPTY_STRING_LIST;
//		}
//		if(method == null) {
//			method = Method.TO;
//		}
//		Result result = getAnalysis(method).parseStr(content);
//		List<Term> terms = result==null?null:result.getTerms();
//		if(terms==null || terms.size()==0) {
//			return EMPTY_STRING_LIST;
//		}
//		List<String> list = new ArrayList<>();
//		for(Term term : terms) {
//			String name = term.getName();
//			if(StringUtil.isBlank(name)) {
//				continue;
//			}
//			list.add(name);
//		}
//		return list;
//	}
//	
//	/** 计算文章摘要
//	 * @param tagRed true 关键词标红
//	 */
//	@SuppressWarnings({"unchecked","rawtypes"})
//	public static String summary(String title, String content, boolean tagRed, Method method) {
//		SummaryComputer sc = new SummaryComputer(title, content);
//		KeyWordComputer kc = new KeyWordComputer(10, getAnalysis(method));
//		List keywords = kc.computeArticleTfidf(title, content);
//		Summary summary = sc.toSummary(keywords);
//		TagContent tagContent = tagRed ? new TagContent("<font color=\"red\">", "</font>") : new TagContent("", "");
//		return tagContent.tagContent(summary);
//	}
//	
//	/** 简繁转换
//	 * @param fan true 简转繁 false 繁转简
//	 */
//	public static String jianfan(String content, boolean fan) {
//		return fan ? JianFan.j2f(content) : JianFan.f2j(content);
//	}
//	
//	/** 获取拼音
//	 * @param content 中文句子
//	 * @param caseType 0-lower 1-camel 2-upper
//	 * @param toneType 0-mark 1-no 2-number 3-abbr （3拼音首字母）
//	 * @param vcharType 0-ü 1-v 2-u: （toneType=0时强制为0）
//	 */
//	public static List<String> pinyin(String content, int caseType, int toneType, int vcharType) {
//		if(StringUtil.isBlank(content)) {
//			return EMPTY_STRING_LIST;
//		}
//		int two = 2;
//		if(toneType!=1 && toneType!=two) {
//			vcharType=0;
//		}
//		List<String> pinyin = Pinyin.tonePinyin(content);
//		if(caseType==0&&toneType==two&&vcharType==two) {
//			return pinyin;
//		}
//		PinyinFormat format = new PinyinFormat(vcharType==1?YuCharType.WITH_V:(vcharType==two?YuCharType.WITH_U_AND_COLON:YuCharType.WITH_U_UNICODE)
//				, toneType==1?ToneType.WITHOUT_TONE:(toneType==two?ToneType.WITH_TONE_NUMBER:(toneType==3?ToneType.WITH_ABBR:ToneType.WITH_TONE_MARK))
//				, caseType==1?CaseType.CAPITALIZE:(caseType==two?CaseType.UPPERCASE:CaseType.LOWERCASE));
//		List<String> list = new ArrayList<>();
//		for(String py : pinyin) {
//			list.add(py==null?py:PinyinFormatter.formatPinyin(py, format));
//		}
//		return list;
//	}
}
