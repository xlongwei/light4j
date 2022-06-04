package com.xlongwei.light4j.handler.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONArray;
import com.google.common.collect.Maps;
import com.networknt.utility.StringUtils;
import com.networknt.utility.Tuple;
import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.EcdictUtil;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.JsonUtil;
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
		String path = exchange.getAttachment(AbstractHandler.PATH);
		if("sort".equals(path)) {
			sort(exchange);
			return;
		}
		String text = HandlerUtil.getParamOrBody(exchange, "text");
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
						List<Tuple<String, Integer>> list = PinyinHelper2.list(sentence);
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

	private void sort(HttpServerExchange exchange) {
		JSONArray array = getArray(exchange);
		if(array==null || array.isEmpty()) {
			return;
		}
		int caseType = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "caseType"), 0);
		int toneType = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "toneType"), 2);
		int vcharType = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "vcharType"), 1);
		Map<Object, String> pys = new HashMap<>();
		array.forEach(o -> pys.put(o, (o!=null && o instanceof String) ? String.join(StringUtils.EMPTY, PinyinUtil.getPinyin((String)o, caseType, toneType, vcharType)) : StringUtils.EMPTY));
		array.sort((o1,o2) -> {
			return pys.get(o1).compareTo(pys.get(o2));
		});
		Map<String,Object> map = Maps.newHashMapWithExpectedSize(4);
		map.put("array", array);
		if(NumberUtil.parseBoolean(HandlerUtil.getParam(exchange, "pinyin"), false)) {
			map.put("pinyin", array.stream().map(pys::get).collect(Collectors.toList()));
		}
		HandlerUtil.setResp(exchange, map);
	}

	private JSONArray getArray(HttpServerExchange exchange) {
		Object array = HandlerUtil.getObject(exchange, "array", Object.class);
		if(array != null) {
			return JsonUtil.parseArray(array instanceof String ? (String)array : JSONArray.toJSONString(array));
		}
		return JsonUtil.parseArray(HandlerUtil.getBodyString(exchange));
	}
}
