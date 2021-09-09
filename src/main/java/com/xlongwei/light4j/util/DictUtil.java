//package com.xlongwei.light4j.util;
//
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.ObjectInputStream;
//import java.io.ObjectOutputStream;
//import java.io.Serializable;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.Map.Entry;
//
//import com.xlongwei.light4j.util.AdtUtil.Queue;
//import com.xlongwei.light4j.util.AdtUtil.Queue.PriorityQueueWithComparator;
//import com.xlongwei.light4j.util.AdtUtil.Queue.LimitSizeQueue;
//import com.xlongwei.light4j.util.FileUtil.CharsetNames;
//import com.xlongwei.light4j.util.FileUtil.TextReader;
//
//import lombok.extern.slf4j.Slf4j;
//
///**
// * dict util
// * @author xlongwei
// *
// */
//@Slf4j
//public class DictUtil {
//	private static final int TWO = 2;
//	private static final String 整体 = "整体";
//	private static Map<String, List<String>> wordMapParts = new HashMap<String, List<String>>();
//	private static Map<List<String>, List<String>> partsMapWords = new HashMap<List<String>, List<String>>();
//	private static Map<String, Integer> simMapRowNumber = new HashMap<String, Integer>();
//	private static Map<String, List<String>> simMapRowWords = new HashMap<String, List<String>>();
//	private static Map<String, String> wordsMapPiece = new HashMap<String, String>();
//	private static Map<String, Integer> guards = new HashMap<String, Integer>();
//	private static Queue<WordScore> results = new LimitSizeQueue<>(10, new PriorityQueueWithComparator<>(Collections.reverseOrder()));
//	private static List<String> frequents = new ArrayList<>();
//	private static Map<String, Integer> strokes = new HashMap<>();
//	private static List<String> simpleWords = new ArrayList<>();
//	private static int simpleWordsStrokes = 12;
//	private static boolean inited = false;
//	private static boolean quick = false;//quick=true时计算partsMapWords，但会占用更多内存
//	
//	static {
//		if(!(inited=initFromDat())) {
//			inited = initFromTxt();
//		}
//		if(inited) {
//			build();
//		}
//	}
//	
//	/**
//	 * <li>日+月
//	 * <li>月 月 鸟
//	 */
//	public static List<WordScore> parse(String input) {
//		if(!inited) {
//			return null;
//		}
//		synchronized (results) {
//			parseInput(input, quick);
//			List<WordScore> list = new ArrayList<>(results.size());
//			WordScore wordScore = null;
//			while((wordScore = results.deQueue()) != null) {
//				list.add(wordScore);
//			}
//			return list;
//		}
//	}
//	
//	/** 笔画数，0 未知 */
//	public static int stroke(String word) {
//		Integer stroke = strokes.get(word);
//		return stroke==null ? 0 : stroke.intValue();
//	}
//	
//	/** 常用简体字 */
//	public static String frequent() {
//		return StringUtil.join(frequents, null, null, null);
//	}
//	
//	/** 简单拆分字 */
//	public static String simple() {
//		return StringUtil.join(simpleWords, null, null, null);
//	}
//	
//	/** 简单拆分字：明 => 日 月 */
//	public static String[] parts(String word) {
//		List<String> parts = wordMapParts.get(word);
//		return parts==null||parts.size()<4 ? null : parts.subList(3, parts.size()).toArray(new String[0]);
//	}
//	
//	public static boolean dumpToDat() {
//		String dat = ConfigUtil.DIRECTORY+"dict.dat";
//		boolean renameTo = new File(dat).renameTo(new File(dat+"."+System.currentTimeMillis()));
//		log.info("dumpToDat: {}, renameTo: {}", dat, renameTo);
//		try(ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(dat))){
//			out.writeObject((Serializable)wordMapParts);
//			out.writeObject((Serializable)wordsMapPiece);
//			out.writeObject((Serializable)simMapRowWords);
//			out.writeObject((Serializable)simMapRowNumber);
//			return true;
//		}catch (Exception e) {
//			return false;
//		}
//	}
//	
//	public static boolean reload(boolean fromTxt) {
//		wordMapParts.clear(); wordsMapPiece.clear(); partsMapWords.clear(); simMapRowNumber.clear(); simMapRowWords.clear(); guards.clear();
//		inited = fromTxt ? initFromTxt() : initFromDat();
//		if(inited) {
//			build();
//		}
//		log.info("reload from txt: "+fromTxt+", inited: "+inited);
//		return inited;
//	}
//	
//	@SuppressWarnings("unchecked")
//	private static boolean initFromDat() {
//		try(ObjectInputStream in = new ObjectInputStream(ConfigUtil.stream("dict.dat"))){
//			wordMapParts = (Map<String, List<String>>) in.readObject();
//			log.info("wordMapParts:" + wordMapParts.size());
//			wordsMapPiece = (Map<String, String>) in.readObject();
//			log.info("wordsMapPiece:" + wordsMapPiece.size());
//			simMapRowWords = (Map<String, List<String>>) in.readObject();
//			log.info("simMapRowWords:" + simMapRowWords.size());
//			simMapRowNumber = (Map<String, Integer>) in.readObject();			
//			return true;
//		}catch (Exception e) {
//			return false;
//		}
//	}
//	
//	private static boolean initFromTxt() {
//		try {
//			loadPart();
//			loadPiece();
//			loadSimilar();
//			return true;
//		}catch (Exception e) {
//			return false;
//		}
//	}
//
//	private static void build() {
//		log.info("build partsMapWords...");
//		for (Entry<String, List<String>> entry : wordMapParts.entrySet()) {
//			String key = entry.getKey();
//			List<String> value = entry.getValue();
//			int lineNumber = Integer.parseInt(value.get(0));
//			ArrayList<String> parts = getParts(key, lineNumber);
//			if (parts.isEmpty()) {
//				log.info(" <- " + value);
//			} else if(quick) {
//				List<String> words = partsMapWords.get(parts);
//				if(words == null) {
//					partsMapWords.put(parts, words = new ArrayList<>());
//				}
//				words.add(key);
//			}
//		}
//		log.info("< {} groups>...over! quick={}", partsMapWords.size(), quick);
//		List<String> readLines = FileUtil.readLines(ConfigUtil.stream("strokes.txt"), CharsetNames.UTF_8);
//		for(String line : readLines) {
//			String[] split = line.split("\\s+");
//			if(split.length!=2) {
//				continue;
//			}
//			strokes.put(split[0], Integer.valueOf(split[1]));
//		}
//		readLines = FileUtil.readLines(ConfigUtil.stream("frequent.txt"), CharsetNames.UTF_8);
//		boolean nofrequent = readLines.isEmpty();
//		if(nofrequent) {
//			readLines.add("的我他这到时地你说就她过对么都好起如把还样道作种情但现行动位老很法斯知次使被进此话活理特做外孩信物打比便部眼体却利海听许性住难教拉记处让放认接军往何叫快吃妈通战远格呢始达深提清化切找钱紶吗语欢指题该论请球决传保读运院近连朋婚队越观形识送造谈极随收根讲取强计似转称领站刻城故惊脸选建持谁准联妇阿诗独消社确酒治兰仅钟怕功待游突哪倒价响礼块脚改据般破仍注沉校娘须试怀料调议列座除跑假设线温掉初停际致阳验助够证饭顿波庭创谢排朝封板况馆忙河续呼推遇伦护冷剧啊险烟依值汉佛唱沙伯低玩速顾洲姑陈抱权怪律胜份汽洋吸例较职渐赶冲胡喝遗救担戏伊村词短规迷旧适投铁博超洗冰状乱顶野按犯拍征坏彩环姆换技洛缺忆判欧付阵玛批狗休恋拥娜妙探呀退诺银康供优课喊降刘健败伴挥财孤枪伙迹遍副坦牌顺秋授浪听预奶雄济烧误");
//		}
//		for(String line : readLines) {
//			char[] cs = line.toCharArray();
//			for(char c:cs) {
//				if(Character.isWhitespace(c)) {
//					continue;
//				}
//				if(StringUtil.isChinese(c)) {
//					frequents.add(String.valueOf(c));
//				}
//			}
//		}
//		if(nofrequent) {
//			simpleWords.addAll(frequents);
//		} else {
//			for(String word : frequents) {
//				if(word.length()>1) {
//					continue;
//				}
//				Integer stroke = strokes.get(word);
//				if(stroke==null || stroke>simpleWordsStrokes) {
//					continue;
//				}
//				List<String> parts = wordMapParts.get(word);
//				int size = parts.size();
//				if(size!=5) {
//					continue;
//				}
//				if(!"左右".equals(parts.get(2))) {
//					continue;
//				}
//				if(parts.get(3).length()>1 || parts.get(4).length()>1) {
//					continue;
//				}
////				List<WordScore> parse = parse(parts.get(3)+" "+parts.get(4));
////				if(parse!=null && parse.size()!=1) {
////					continue;
////				}
//				simpleWords.add(word);
//			}
//		}
//		log.info("frequents: {}, strokes: {}, simple words: {}", frequents.size(), strokes.size(), simpleWords.size());
//	}
//
//	private static int count(List<String> parts, String key) {
//		int count = 0;
//		for (String part : parts) {
//			if (part.equals(key)) {
//				count++;
//			}
//		}
//		return count;
//	}
//
//	private static List<String> getParts(String word) {
//		List<String> parts = new LinkedList<String>();
//		if (guards.containsKey(word)) {
//			log.info("unlimited: " + word + ":" + guards.get(word));
//			return parts;
//		} else if (StringUtil.isNumbers(word)) {
//			parts.add(word);
//			return parts;
//		} else if (!wordMapParts.containsKey(word)) {
//			log.info("no explain: " + word);
//			parts.add(word);
//			return parts;
//		} else if (wordMapParts.get(word).get(TWO).equals(整体)) {
//			parts.add(word);
//			return parts;
//		} else {
//			guards.put(word, Integer.parseInt(wordMapParts.get(word).get(0)));
//			for (int index = 3; index < wordMapParts.get(word).size(); index++) {
//				List<String> temp = getParts(wordMapParts.get(word).get(index));
//				if (temp.isEmpty()) {
//					parts.clear();
//					log.info(" <- " + word + ":" + guards.get(word));
//					break;
//				} else {
//					parts.addAll(temp);
//				}
//			}
//			guards.remove(word);
//			return parts;
//		}
//	}
//
//	private static ArrayList<String> getParts(String word, int lineNumber) {
//		ArrayList<String> parts = new ArrayList<String>();
//		if (guards.containsKey(word)) {
//			log.info("unlimited: " + word + ":" + guards.get(word));
//			return parts;
//		} else if (StringUtil.isNumbers(word)) {
//			parts.add(word);
//			return parts;
//		} else if (!wordMapParts.containsKey(word)) {
//			log.info("no explain: " + word + ":" + lineNumber);
//			parts.add(word);
//			return parts;
//		} else if (wordMapParts.get(word).get(TWO).equals(整体)) {
//			parts.add(word);
//			return parts;
//		} else {
//			guards.put(word, lineNumber);
//			for (int index = 3; index < wordMapParts.get(word).size(); index++) {
//				ArrayList<String> temp = getParts(wordMapParts.get(word).get(index), lineNumber);
//				if (temp.isEmpty()) {
//					parts.clear();
//					log.info(" <- " + word + ":" + guards.get(word));
//					break;
//				} else {
//					parts.addAll(temp);
//				}
//			}
//			guards.remove(word);
//			return parts;
//		}
//	}
//
//	private static void loadPart() {
//		log.info("load part.txt ...");
//		TextReader reader = new TextReader(ConfigUtil.stream("part.txt"), CharsetNames.UTF_8);
//		int lineNumber = 0;
//		String line = null;
//		while ((line = reader.read()) != null) {
//			lineNumber++;
//			List<String> parts = new ArrayList<String>();
//			parts.add(String.valueOf(lineNumber));
//			String[] words = line.split("\\s+");
//			if (words.length < 3) {
//				log.info("wrong line: " + lineNumber);
//			} else {
//				String word = words[0];
//				if (wordMapParts.containsKey(word)) {
//					log.info("reduplicate: {}:{} <-> {}:{}", lineNumber, word, wordMapParts.get(word).get(0), wordMapParts.get(word).get(1));
//				} else if ((words.length == 3)
//						&& (!words[1].equals(整体))) {
//					log.info("miss parts: {} -> {}", lineNumber, Arrays.toString(words));
//				} else if ((words.length > 3) && words[1].equals(整体)) {
//					log.info("too many parts: {}->{}", lineNumber, Arrays.toString(words));
//				} else {
//					for (int index = 0; index < words.length; index++) {
//						parts.add(words[index]);
//					}
//					wordMapParts.put(word, parts);
//				}
//			}
//		}
//	}
//
//	private static void loadPiece() {
//		log.info("load piece.txt...");
//		TextReader reader = new TextReader(ConfigUtil.stream("piece.txt"), CharsetNames.UTF_8);
//		int lineNumber = 0;
//		String line = null;
//		while ((line = reader.read()) != null) {
//			lineNumber++;
//			ArrayList<String> parts = new ArrayList<String>();
//			parts.add(String.valueOf(lineNumber));
//			String[] words = line.split("\\s+");
//			if (words.length >= 2) {
//				for (int index = 1; index < words.length; index++) {
//					wordsMapPiece.put(words[index], words[0]);
//				}
//			} else {
//				log.info("wrong line: " + lineNumber);
//			}
//		}
//		log.info("<" + wordsMapPiece.size() + " parts>...over!");
//	}
//
//	private static void loadSimilar() {
//		log.info("load similar.txt...");
//		TextReader reader = new TextReader(ConfigUtil.stream("similar.txt"), CharsetNames.UTF_8);
//		int lineNumber = 0;
//		String line = null;
//		while ((line = reader.read()) != null) {
//			lineNumber++;
//			ArrayList<String> parts = new ArrayList<String>();
//			parts.add(String.valueOf(lineNumber));
//			String[] words = line.split("\\s+");
//			ArrayList<String> similarRow = new ArrayList<String>();
//			for (String word : words) {
//				if (simMapRowNumber.containsKey(word)) {
//					log.info("reduplicate: " + word + ":" + lineNumber + "<->" + simMapRowNumber.get(word));
//				} else {
//					simMapRowNumber.put(word, lineNumber);
//					similarRow.add(word);
//				}
//			}
//			if (similarRow.size() > 1) {
//				for (String word : similarRow) {
//					simMapRowWords.put(word, similarRow);
//				}
//			} else {
//				log.info("wrong line: " + lineNumber);
//				for (String word : similarRow) {
//					simMapRowNumber.remove(word);
//				}
//			}
//		}
//		reader.close();
//		log.info("<" + simMapRowWords.size() + " similar rows>...over!");
//	}
//
//	private static void parseInput(String line, boolean quick) {
//		List<String> partsOfInput = new LinkedList<String>();
//		List<List<String>> parts = new LinkedList<>();
//		parts.add(partsOfInput);
//		line = StringUtil.toDBC(line);
//		String[] pieces = line.split("([\\s\\+])+");
//		for (int index = 0; index < pieces.length; index++) {
//			String piece = pieces[index];
//			List<String> words = process(piece);
//			if ((words != null) && (words.size() != 0)) {
//				for (String word : words) {
//					List<String> list = simMapRowWords.get(word);
//					if(list!=null && list.size()>0) {
//						List<List<String>> partsSimilars = new LinkedList<>();
//						for(String similar : list) {
//							if(similar.equals(word)) {
//								continue;
//							}
//							List<String> partsOfWord = getParts(similar);
//							for(List<String> partsSimilar : parts) {
//								partsSimilar = new LinkedList<>(partsSimilar);
//								partsSimilar.addAll(partsOfWord);
//								partsSimilars.add(partsSimilar);
//							}
//						}
//						List<String> partsOfWord = getParts(word);
//						for(List<String> partsSimilar : parts) {
//							partsSimilar.addAll(partsOfWord);
//						}
//						parts.addAll(partsSimilars);
//					}else {
//						List<String> partsOfWord = getParts(word);
//						for(List<String> partsSimilar : parts) {
//							partsSimilar.addAll(partsOfWord);
//						}
//					}
//				}
//			}
//		}
//		if (partsOfInput.isEmpty()) {
//			return;
//		} else {
//			if (quick) {
//				Set<String> words = new HashSet<>();
//				for(List<String> partsSimilar : parts) {
//					List<String> list = partsMapWords.get(partsSimilar);
//					if(list!=null && list.size()>0) {
//						for(String word : list) {
//							if(word.length()==1) {
//								words.add(word);
//							}
//						}
//					}
//				}
//				for(String word : words) {
//					results.enQueue(new WordScore(word, 1.0));
//				}
//			}
//			if(results.isEmpty()) {
//				parseParts(partsOfInput);
//			}
//		}
//	}
//
//	private static void parseParts(List<String> parts) {
//		List<String> temp = new LinkedList<String>();
//		for (String key : wordMapParts.keySet()) {
//			if (key.length() > 1) {
//				continue;
//			}
//			List<String> partsOfKey = getParts(key);
//			int fenzi = 0, fenmu = parts.size() + partsOfKey.size();
//			temp.clear();
//			for (String part : parts) {
//				if (!temp.contains(part) && partsOfKey.contains(part)) {
//					fenzi += 2 * Math.min(count(parts, part), count(partsOfKey, part));
//					temp.add(part);
//				}
//			}
//			results.enQueue(new WordScore(key, (double) fenzi / fenmu));
//		}
//	}
//
//	private static ArrayList<String> partOfWord(String word, int type) {
//		ArrayList<String> part = new ArrayList<String>();
//		word = word.trim();
//		if (word.isEmpty() || !wordMapParts.containsKey(word)) {
//			return part;
//		} else {
//			switch (wordMapParts.get(word).size()) {
//			case 4:
//				part.add(wordMapParts.get(word).get(3));
//				break;
//			case 5:
//				part.add(wordMapParts.get(word).get(type > 1 ? 4 : 3));
//				break;
//			case 6:
//				part.add(wordMapParts.get(word).get(2 + type));
//				break;
//			default:
//				break;
//			}
//			return part;
//		}
//	}
//
//	private static ArrayList<String> partsOfWord(String str) {
//		ArrayList<String> parts = new ArrayList<String>();
//		ArrayList<String> strs = new ArrayList<String>();
//		int bFlag = 0, from = 0, to = 0;
//		while (to < str.length()) {
//			if (str.charAt(to) == '(') {
//				bFlag++;
//			} else if (str.charAt(to) == ')') {
//				bFlag--;
//			} else if ((str.charAt(to) == ',') && (bFlag == 0)) {
//				strs.add(str.substring(from, to));
//				from = to + 1;
//			}
//			to++;
//		}
//		if (from < to) {
//			strs.add(str.substring(from, to));
//		}
//		for (String s : strs) {
//			parts.addAll(process(s));
//		}
//		return parts;
//	}
//
//	/**
//	 * @param str 左右（日，月）
//	 * @return 日 月
//	 */
//	private static ArrayList<String> process(String str) {
//		ArrayList<String> wordList = new ArrayList<String>();
//		int posOfLeftP;
//		posOfLeftP = str.indexOf("(");
//		if (posOfLeftP < 0) {
//			String comma = ",";
//			for (String s : str.split(comma)) {
//				if (wordsMapPiece.containsKey(s)) {
//					wordList.add(wordsMapPiece.get(s));
//				} else if (wordMapParts.containsKey(s)) {
//					wordList.add(s);
//				} else {
//					log.info("不能识别: " + s);
//				}
//			}
//			return wordList;
//		} else {
//			String prestr = str.substring(0, posOfLeftP);
//			String rightP = ")";
//			if (!str.endsWith(rightP)) {
//				log.info("缺少括号: " + str + "~");
//				return wordList;
//			} else {
//				int three = 3;
//				if ((str.length() - posOfLeftP) < three) {
//					return wordList;
//				}
//				String midstr = str.substring(posOfLeftP + 1, str.length() - 1);
//				String first = "上,左,外", second = "中,里", third = "下,右", fourth = "左右,上下,外里,重复,组合";
//				if (StringUtil.splitContains(first, prestr)) {
//					wordList.addAll(partOfWord(midstr, 1));
//				} else if (StringUtil.splitContains(second, prestr)) {
//					wordList.addAll(partOfWord(midstr, 2));
//				} else if (StringUtil.splitContains(third, prestr)) {
//					wordList.addAll(partOfWord(midstr, 3));
//				} else if (StringUtil.splitContains(fourth, prestr)) {
//					wordList.addAll(partsOfWord(midstr));
//				} else {
//					log.info("格式错误：{}", str);
//				}
//				return wordList;
//			}
//		}
//	}
//
//	public static class WordScore implements Comparable<WordScore> {
//		public WordScore(String word, Double score) { this.word = word; this.score = score; }
//		@Override
//		public int compareTo(WordScore o) { return score.compareTo(o.score); }
//		public Double getScore() { return score; }
//		public String getWord() { return word; }
//		@Override
//		public String toString() { return word+":"+score; }
//		private String word; private Double score;
//	}
//}
