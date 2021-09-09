//package com.xlongwei.light4j.handler.weixin;
//
//import java.util.Arrays;
//import java.util.List;
//
//import com.xlongwei.light4j.util.DictUtil;
//import com.xlongwei.light4j.util.DictUtil.WordScore;
//import com.xlongwei.light4j.util.PinyinUtil;
//import com.xlongwei.light4j.util.StringUtil;
//import com.xlongwei.light4j.util.WeixinUtil.AbstractMessageHandler.AbstractTextHandler;
//
///**
// * dict handler
// * @author xlongwei
// *
// */
//public class DictHandler extends AbstractTextHandler {
//	private String regex = "^(查字|字典|生字|查询|生僻字)[ 　]+[\\u4E00-\\u9FA5]+(([+,;＋，；、]|[ 　]+)[\\u4E00-\\u9FA5]+)*$";
//
//	@Override
//	public String handle(String content) {
//		if(content.matches(regex)) {
//			StringBuilder answer = new StringBuilder();
//			content = content.substring(3).trim();
//			List<WordScore> list = DictUtil.parse(content);
//			for(WordScore word : list) {
//				answer.append(word.getWord()+"  ");
//				String[] py = PinyinUtil.getPinyin(word.getWord().charAt(0), 0, 0, 0);
//				answer.append(StringUtil.join(Arrays.asList(py), null, null, ", "));
//				answer.append("\n");
//			}
//			if(answer.length() > 0) {
//				return answer.substring(0, answer.length()-1);
//			}
//		}
//		return null;
//	}
//
//}
