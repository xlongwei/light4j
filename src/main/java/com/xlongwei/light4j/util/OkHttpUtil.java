package com.xlongwei.light4j.util;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.X509TrustManager;

import com.xlongwei.light4j.util.FileUtil.FileItem;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * okhttp util
 * @author xlongwei
 */
@Slf4j
public class OkHttpUtil {
	public static OkHttpClient okHttpClient = null;
	public static MediaType fileType = MediaType.parse("application/octet-stream");
	
	public static String post(String api, Map<String, String> params, FileItem ... fileItems) {
		log.info("post: {}", api);
		boolean hasParams = params!=null && params.size()>0;
		boolean hasFileItems = fileItems!=null && fileItems.length>0;
		Request request = null;
		Request.Builder builder = new Request.Builder().url(api);
		if(hasFileItems) {
			MultipartBody.Builder multipartBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
			if(hasParams) {
				log.info("with params: {}", params.toString());
				for(String name : params.keySet()) {
					String value = params.get(name);
					if(value!=null && (value=value.trim()).length()>0) {
						multipartBuilder.addFormDataPart(name, value);
					} else {
						log.info("empty param omitted: {}", name);
					}
				}
			}
			if(hasFileItems) {
				for(FileItem fileItem : fileItems) {
					if(fileItem.file.exists()==false || fileItem.file.isFile()==false) {
						log.info("upload file ommited, file not exists or not file");
					}else {
						multipartBuilder.addFormDataPart(fileItem.name, fileItem.file.getName(), RequestBody.create(fileType, fileItem.file));
						log.info("upload name: {}, file: {}", fileItem.name, fileItem.file.getAbsolutePath());
					}
				}
			}
			request = builder.post(multipartBuilder.build()).build();
		}else {
			FormBody.Builder formBodyBuilder = new FormBody.Builder();
			if(hasParams) {
				log.info("with params: {}", params.toString());
				for(String name : params.keySet()) {
					String value = params.get(name);
					if(value!=null && (value=value.trim()).length()>0) {
						formBodyBuilder.add(name, value);
					} else {
						log.info("empty param omitted: {}", name);
					}
				}
			}
			request = builder.post(formBodyBuilder.build()).build();
		}
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicReference<Response> reference = new AtomicReference<>();
		long s = System.currentTimeMillis();
		okHttpClient.newCall(request).enqueue(new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				log.warn("failed with ex: {}", e.getMessage());
				latch.countDown();
			}
			@Override
			public void onResponse(Call call, Response response) throws IOException {
				reference.set(response);
				latch.countDown();
			}
		});
		try{
			latch.await();
			Response response = reference.get();
			if(reference!=null) {
				int status = response.code();
				String string = response.body()==null ? null : response.body().string();
				long e = System.currentTimeMillis();
				log.info("status: {}, elapsed: {}ms, protocal: {}", status, e-s, response.protocol());
				log.info("result: {}", string);
				return string;
			}
		}catch(Exception e) {
			log.warn("failed with ex: {}", e.getMessage());
		}
		return null;
	}
	
	static {
		okHttpClient = new OkHttpClient.Builder()
    			.sslSocketFactory(FileUtil.sslContext.getSocketFactory(), (X509TrustManager) FileUtil.trustAllCerts[0])
    			.hostnameVerifier(FileUtil.verifyAllHosts)
    			.build();
		log.info("OkHttpClient 3.10.0 prepared.");
	}
}
