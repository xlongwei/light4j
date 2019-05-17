package com.xlongwei.light4j.handler.weixin;

import java.util.Arrays;

import com.xlongwei.light4j.util.PinyinUtil;
import com.xlongwei.light4j.util.StringUtil;
import com.xlongwei.light4j.util.WeixinUtil.AbstractMessageHandler.AbstractTextHandler;

/**
 * 获取拼音
 * @author xlongwei
 *
 */
public class PinyinHandler extends AbstractTextHandler {
	private String regex = "^拼音"+split+"(.+)$";

	@Override
	public String handle(String content) {
		if(content.length()==1 && StringUtil.isChinese(StringUtil.toDBC(content).charAt(0))) {
			StringBuilder answer = new StringBuilder();
			String[] py = PinyinUtil.getPinyin(content.charAt(0), 0, 0, 0);
			answer.append(content+"  ");
			answer.append(StringUtil.join(Arrays.asList(py), null, null, ", "));
			return answer.toString();
		}else if(content.matches(regex)) {
			String text = content.substring(3).trim();
			String[] pinyin = PinyinUtil.getPinyin(text, 0, 0, 0);
			return StringUtil.join(Arrays.asList(pinyin), null, null, " ");
		}
		return null;
	}

}
