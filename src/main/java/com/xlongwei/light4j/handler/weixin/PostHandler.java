package com.xlongwei.light4j.handler.weixin;

import java.util.HashSet;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;

import com.xlongwei.light4j.util.IdCardUtil;
import com.xlongwei.light4j.util.PostUtil;
import com.xlongwei.light4j.util.StringUtil;
import com.xlongwei.light4j.util.WeixinUtil.AbstractMessageHandler.AbstractTextHandler;

/**
 * post handler
 * @author xlongwei
 *
 */
public class PostHandler extends AbstractTextHandler {

	private static final String TAG = "邮编";
	private Pattern pattern = Pattern.compile("\\d{6}");
	
	@Override
	public String handle(String content) {
		if(StringUtil.isBlank(content)) {
			return null;
		}
		
		if(pattern.matcher(content).matches()) {
			return post(content);
		}
		if(!content.startsWith(TAG)) {
			return null;
		}
		
		String post = content.substring(2).trim();
		if(StringUtil.isBlank(post)) {
			return null;
		}
		if(pattern.matcher(post).matches()) {
			return post(post);
		}
		
		for(String code : PostUtil.posts.allL()) {
			HashSet<String> cities = PostUtil.posts.getR(code);
			for(String city : cities) {
				if(city.startsWith(post)) {
					return code;
				}
			}
		}
		return null;
	}

	private String post(String code) {
		HashSet<String> cities = PostUtil.posts.getR(code);
		if(CollectionUtils.isNotEmpty(cities)) {
			return new StringBuilder("邮编：").append(StringUtil.join(cities, null, null, null)).toString();
		}
		String area = StringUtil.join(IdCardUtil.areas(code), null, null, null);;
		if(StringUtil.hasLength(area)) {
			return new StringBuilder("行政区：").append(area).toString();
		}
		return null;
	}
}
