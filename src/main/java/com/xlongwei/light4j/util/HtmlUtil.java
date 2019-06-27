package com.xlongwei.light4j.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

import lombok.extern.slf4j.Slf4j;

/**
 * html util
 * @author xlongwei
 *
 */
@Slf4j
public class HtmlUtil {
	public static Pattern pattern = Pattern.compile("<meta.*charset=[\"\']?([0-9a-zA-Z-]+)[\"\']", Pattern.CASE_INSENSITIVE);

	/** 探测字节文本的编码 */
	public static String charset(byte[] bs) {
		if(bs==null || bs.length==0) {
			return null;
		}
		String string = new String(bs, StandardCharsets.ISO_8859_1);
		String charset = StringUtil.getPatternString(string, pattern);
		//charset声明编码可以优先确定探测编码正确
		CharsetDetector charsetDetector = new CharsetDetector();
		charsetDetector.setText(bs);
		CharsetMatch[] matchs = charsetDetector.detectAll();
		if(matchs!=null && matchs.length>0) {
			int len = matchs.length - 1;
			for(int i=0; i<len; i++) {
				if(charset!=null && charset.equalsIgnoreCase(matchs[i].getName())) {
					return charset;
				}
				if(matchs[i+1].getConfidence()<matchs[i].getConfidence()) {
					return matchs[i].getName();
				}
			}
			return matchs[len].getName();
		}
		return null;
	}
	
	/** 探测文本流的编码 */
	public static String charset(InputStream is) {
		try {
			int kBufSize = 8000;
			int fRawLength = 0;
			byte[] fRawInput = new byte[kBufSize];
			if(is.markSupported()) {
				is.mark(kBufSize);
			}
			int remainingLength = kBufSize;
	        while (remainingLength > 0 ) {
	            int  bytesRead = is.read(fRawInput, fRawLength, remainingLength);
	            if (bytesRead <= 0) {
	                 break;
	            }
	            fRawLength += bytesRead;
	            remainingLength -= bytesRead;
	        }
	        if(is.markSupported()) {
	        	is.reset();
	        }
	        return charset(fRawInput);
		} catch (Exception e) {
			log.warn("fail to charset InputStream: {}, ex: {}", is, e.getMessage());
		}
		return null;
	}
	
	/** 探测网址的编码 */
	public static String charset(String url) {
		try {
			HttpUriRequest request = RequestBuilder.get(url).build();
			HttpResponse response = HttpUtil.httpClient.execute(request);
			HttpEntity entity = response.getEntity();
			byte[] bs = EntityUtils.toByteArray(entity);
			Charset charset = null;
			ContentType contentType = ContentType.get(entity);
			if(contentType != null) {
				charset = contentType.getCharset();
			}
			if(charset == null) {
				String charsetName = charset(bs);
				if(charsetName != null) {
					charset = Charset.forName(charsetName);
				}
			}
			if(charset != null) {
				return charset.name();
			}
		}catch(Exception e) {
			log.warn("fail to charset url: {}, ex: {}", url, e.getMessage());
		}
		return null;
	}
	
	/** 探测文本文件的编码 */
	public static String charset(File file) {
		try{
			return charset(new BufferedInputStream(new FileInputStream(file)));
		}catch(Exception e) {
			log.warn("fail to charset file: {}, ex: {}", file, e.getMessage());
			return null;
		}
	}
	
	/** 获取字节文本的字符串 */
	public static String string(byte[] bs) {
		if(bs==null || bs.length==0) {
			return null;
		}
		String charset = charset(bs);
		try{
			return charset==null ? null : new String(bs, charset);
		}catch(Exception e) {
			log.warn("fail to string byte[].length: {}, charset: {}, ex: {}", bs.length, charset, e.getMessage());
			return null;
		}
	}
	
	/** 获取文本流的字符串 */
	public static String string(InputStream is) {
		byte[] bs = FileUtil.readStream(is).toByteArray();
		return string(bs);
	}
	
	/** 获取文本文件的字符串 */
	public static String string(File file) {
		try{
			return string(new BufferedInputStream(new FileInputStream(file)));
		}catch(Exception e) {
			log.warn("fail to string file: {}, ex: {}", file, e.getMessage());
			return null;
		}
	}
	
	/** 获取网址的字符串 */
	public static String get(String url, Header ... headers) {
		try {
			RequestBuilder requestBuilder = RequestBuilder.get(url);
			if(headers!=null && headers.length>0) {
				for(Header header : headers) {
					requestBuilder.addHeader(header);
				}
			}
			HttpUriRequest request = requestBuilder.build();
			HttpResponse response = HttpUtil.httpClient.execute(request);
			HttpEntity entity = response.getEntity();
			byte[] bs = EntityUtils.toByteArray(entity);
			Charset charset = null;
			ContentType contentType = ContentType.get(entity);
			if(contentType != null) {
				charset = contentType.getCharset();
			}
			if(charset == null) {
				String charsetName = charset(bs);
				if(charsetName != null) {
					charset = Charset.forName(charsetName);
				}
			}
			if(charset == null) {
				charset = HTTP.DEF_CONTENT_CHARSET;
			}
			return new String(bs, charset);
		}catch(Exception e) {
			log.warn("fail to get url: {}, ex: {}", url, e.getMessage());
		}
		return null;
	}
	
	/** 获取post请求后的响应字符串 */
	public static String post(String url, Map<String, String> headers, Map<String, String> params) {
		try {
			RequestBuilder post = RequestBuilder.post(url);
			boolean hasHeaders = headers!=null && headers.size()>0;
			boolean hasParams = params!=null && params.size()>0;
			if(hasHeaders) {
				for(String name : headers.keySet()) {
					String value = headers.get(name);
					if(value!=null && (value=value.trim()).length()>0) {
						post.addHeader(name, value);
					}
				}
			}
			if(hasParams) {
				for(String name : params.keySet()) {
					String value = params.get(name);
					if(value!=null && (value=value.trim()).length()>0) {
						post.addParameter(name, value);
					}
				}
			}
			HttpUriRequest request = post.build();
			HttpResponse response = HttpUtil.httpClient.execute(request);
			HttpEntity entity = response.getEntity();
			byte[] bs = EntityUtils.toByteArray(entity);
			Charset charset = null;
			ContentType contentType = ContentType.get(entity);
			if(contentType != null) {
				charset = contentType.getCharset();
			}
			if(charset == null) {
				String charsetName = charset(bs);
				if(charsetName != null) {
					charset = Charset.forName(charsetName);
				}
			}
			if(charset == null) {
				charset = HTTP.DEF_CONTENT_CHARSET;
			}
			return new String(bs, charset);
		}catch(Exception e) {
			log.warn("fail to post url: {}, ex: {}", url, e.getMessage());
		}
		return null;
	}
}
