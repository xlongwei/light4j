package com.xlongwei.light4j.util;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.xlongwei.light4j.util.FileUtil.FileItem;

import lombok.extern.slf4j.Slf4j;

/**
 * http util over okhttp
 * @author xlongwei
 *
 */
@Slf4j
public class HttpUtil {
	public static final HttpClient httpClient;
	public static final ContentType txtUtf8 = ContentType.create("text/plain", StandardCharsets.UTF_8);
	
	private static int connectionRequestTimeout = 10000;
	private static int connectionTimeout = 20000;
	private static int socketTimeout = 30000;
	private static int maxConnTotal = 384;
	private static int maxConnPerRoute = maxConnTotal;

	public static String get(String api, Map<String, String> params) {
		log.info("get: {}", api);
		RequestBuilder requestBuilder = RequestBuilder.get(api);
		if(params!=null && params.isEmpty()==false) {
			params.forEach((k, v) -> {
				if(v!=null && v.length()>0) {
					requestBuilder.addParameter(k, v);
				}
			});
			log.info("with params: {}", params.toString());
		}
		return execute(requestBuilder.build());
	}
	
	public static String post(String api, Map<String, String> params, FileItem ... fileItems) {
		MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create().setMode(HttpMultipartMode.RFC6532);
		log.info("post: {}", api);
		if(params!=null && params.size()>0) {
			for(String param : params.keySet()) {
				String value = params.get(param);
				if(value!=null && (value=value.trim()).length()>0) {
					entityBuilder.addTextBody(param, value, txtUtf8);
				} else {
					log.info("empty param omitted: {}", param);
				}
			}
			log.info("with params: {}", params.toString());
		}
		
		if(fileItems!=null && fileItems.length>0) {
			for(FileItem fileItem : fileItems) {
				if(fileItem.file.exists()==false || fileItem.file.isFile()==false) {
					log.info("upload file ommited, file not exists or not file");
				}else {
					entityBuilder.addPart(fileItem.name, new FileBody(fileItem.file));
					log.info("upload name: {}, file: {}", fileItem.name, fileItem.file.getAbsolutePath());
				}
			}
		}
		
		HttpEntity entity = entityBuilder.build();
		HttpUriRequest request = RequestBuilder.post()
				.setUri(api)
				.setEntity(entity)
				.build();
		
		return execute(request);
	}

	private static String execute(HttpUriRequest request) {
		try {
			long s = System.currentTimeMillis();
			HttpResponse response = httpClient.execute(request);
			int status = response.getStatusLine().getStatusCode();
			String string = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
			long e = System.currentTimeMillis();
			log.info("status: {}, elapsed: {}ms", status, e-s);
			log.info("result: {}", string);
			return string;
		}catch(Exception e) {
			log.warn("failed with ex: {}", e.getMessage());
			return null;
		}
	}
	
	static {
		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectionRequestTimeout(connectionRequestTimeout)
				.setConnectTimeout(connectionTimeout)
				.setSocketTimeout(socketTimeout)
				.build();
		httpClient = HttpClients.custom()
				.setDefaultRequestConfig(requestConfig)
				.setMaxConnTotal(maxConnTotal)
				.setMaxConnPerRoute(maxConnPerRoute)
				.setSSLContext(FileUtil.sslContext)
				.build();
	}
}
