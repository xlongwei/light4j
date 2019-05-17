package com.xlongwei.light4j.util;

import java.io.File;
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

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * http util over okhttp
 * @author xlongwei
 *
 */
@Slf4j
public class HttpUtil {
	public static HttpClient httpClient = null;
	public static ContentType txtUtf8 = ContentType.create("text/plain", StandardCharsets.UTF_8);
	
	private static int connectionRequestTimeout = 10000;
	private static int connectionTimeout = 20000;
	private static int socketTimeout = 30000;
	private static int maxConnTotal = 384;
	private static int maxConnPerRoute = maxConnTotal;

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
	
	@AllArgsConstructor
	public static class FileItem {
		private String name;
		private File file;
	}
	
	private static void prepare() {
		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectionRequestTimeout(connectionRequestTimeout)
				.setConnectTimeout(connectionTimeout)
				.setSocketTimeout(socketTimeout)
				.build();
		httpClient = HttpClients.custom()
				.setDefaultRequestConfig(requestConfig)
				.setMaxConnTotal(maxConnTotal)
				.setMaxConnPerRoute(maxConnPerRoute)
				.setSslcontext(FileUtil.sslContext)
				.setUserAgent("JSONClient / HttpClient 4.3")
				.build();
	}
	
	static {
		prepare();
	}
}
