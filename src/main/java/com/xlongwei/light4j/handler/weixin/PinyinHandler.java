package com.xlongwei.light4j.handler.weixin;

import java.util.Arrays;

import com.xlongwei.light4j.util.PinyinUtil;
import com.xlongwei.light4j.util.StringUtil;
import com.xlongwei.light4j.util.WeixinUtil.AbstractMessageHandler.AbstractTextHandler;

import net.sourceforge.pinyin4j.PinyinHelper2;

/**
 * 获取拼音
 * @author xlongwei
 *
 */
public class PinyinHandler extends AbstractTextHandler {
	private String regex = "^拼音"+split+"(.+)$";

	@Override
	public String handle(String content) {
		if(PinyinHelper2.hasPinyin(content)) {
			StringBuilder answer = new StringBuilder();
			String[] py = PinyinUtil.getPinyin(content, 0, 0, 0);
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
