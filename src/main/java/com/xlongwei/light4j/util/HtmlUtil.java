package com.xlongwei.light4j.util;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

/**
 * html util
 * @author xlongwei
 *
 */
public class HtmlUtil {
	public static Pattern pattern = Pattern.compile("<meta.*charset=\"?([0-9a-zA-Z-]+)\"", Pattern.CASE_INSENSITIVE);

	public static String charset(byte[] bs) {
		if(bs==null || bs.length==0) {
			return null;
		}
		String string = new String(bs, StandardCharsets.ISO_8859_1);
		String charset = StringUtil.getPatternString(string, pattern);
		if(charset != null) {
			return charset;
		}
		CharsetDetector charsetDetector = new CharsetDetector();
		charsetDetector.setText(bs);
		CharsetMatch[] matchs = charsetDetector.detectAll();
		if(matchs!=null && matchs.length>0) {
			int len = matchs.length - 1;
			for(int i=0; i<len; i++) {
				if(matchs[i+1].getConfidence()<matchs[i].getConfidence()) {
					return matchs[i].getName();
				}
			}
			return matchs[len].getName();
		}
		return null;
	}
	
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
			//ignore
		}
		return null;
	}
	
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
			
		}
		return null;
	}
	
	public static String get(String url) {
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
			if(charset == null) {
				charset = HTTP.DEF_CONTENT_CHARSET;
			}
			return new String(bs, charset);
		}catch(Exception e) {
			
		}
		return null;
	}
	
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
			
		}
		return null;
	}
}
