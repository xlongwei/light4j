package com.xlongwei.light4j.handler.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.networknt.utility.StringUtils;
import com.networknt.utility.Tuple;
import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.EcdictUtil;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.NumberUtil;
import com.xlongwei.light4j.util.PinyinUtil;
import com.xlongwei.light4j.util.StringUtil;

import io.undertow.server.HttpServerExchange;
import net.sourceforge.pinyin4j.PinyinHelper2;

/**
 * pinyin4j
 * @author xlongwei
 *
 */
public class PinyinHandler extends AbstractHandler {

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		String text = HandlerUtil.getParam(exchange, "text");
		if(StringUtils.isNotBlank(text)) {
			int caseType = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "caseType"), 0);
			int toneType = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "toneType"), 0);
			int vcharType = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "vcharType"), 0);
			
			boolean isWord = PinyinHelper2.isWord(text);
			String[] pinyin = PinyinUtil.getPinyin(text, caseType, toneType, vcharType);
			String join = StringUtil.join(Arrays.asList(pinyin), null, null, " ");
			Map<String, Object> map = new HashMap<>(2);
			map.put("pinyin", join);
			if(!isWord) {
				StringBuilder header = new StringBuilder();
				for(String py : pinyin) {
					header.append(PinyinHelper2.isWord(py) ? py : py.charAt(0));
				}
				map.put("header", header.toString());
				List<String[]> symbols = new LinkedList<>();
				for(String sentence : EcdictUtil.sentences(text)) {
					if(StringUtil.isHasChinese(sentence)) {
						List<Tuple<String, String>> list = PinyinHelper2.list(sentence);
						pinyin = PinyinUtil.getPinyin(sentence, caseType, toneType, vcharType);
						for(int i=0,j=pinyin.length;i<j;i++) {
							symbols.add(new String[] {list.get(i).first, pinyin[i]});
						}
					}else {
						List<String> words = EcdictUtil.words(sentence);
						List<String> pinyins = EcdictUtil.pinyin(words);
						for(int i=0,j=words.size();i<j;i++) {
							symbols.add(new String[] {words.get(i), pinyins.get(i)});
						}
					}
				}
				map.put("words", symbols);
			}
			HandlerUtil.setResp(exchange, map);
		}
	}

}
