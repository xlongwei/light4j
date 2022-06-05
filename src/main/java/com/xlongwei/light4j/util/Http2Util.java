package com.xlongwei.light4j.util;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.networknt.client.Http2Client;
import com.networknt.httpstring.ContentType;
import com.networknt.httpstring.HttpStringConstants;
import com.networknt.utility.NioUtils;

import org.xnio.OptionMap;

import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormData.FormValue;
import io.undertow.util.Headers;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Http2Util {
    /** 请求超时（毫秒） */
    public static long timeout = 30000;
    private static Http2Client client = Http2Client.getInstance();
    private static OptionMap httpsOptionMap = OptionMap.create(UndertowOptions.ENABLE_HTTP2, true);

    /**
     * 普通http请求（非文件上传）
     * 
     * @param uri     http://localhost:8081
     * @param request new ClientRequest().setMethod(Methods.GET).setPath("/rest")
     * @param body    可以为null，普通表单可使用Http2Client.getFormDataString(Map&lt;String,
     *                String> params)
     * @return String
     */
    public static String execute(URI uri, ClientRequest request, String body) {
        ClientConnection connection = null;
        try {
            connection = client.borrowConnection(uri, Http2Client.WORKER, client.getDefaultXnioSsl(),
                    Http2Client.BUFFER_POOL, "https".equals(uri.getScheme()) ? httpsOptionMap : OptionMap.EMPTY).get();
            AtomicReference<ClientResponse> reference = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);
            addHostHeader(request);
            if (body == null || body.isEmpty()) {
                connection.sendRequest(request, client.createClientCallback(reference, latch));
            } else {
                if (!request.getRequestHeaders().contains(Headers.CONTENT_TYPE)) {
                    char c = body.charAt(0);
                    if (c == '{' || c == '[') {
                        request.getRequestHeaders().put(Headers.CONTENT_TYPE, ContentType.APPLICATION_JSON.value());
                    } else {
                        request.getRequestHeaders().put(Headers.CONTENT_TYPE,
                                ContentType.APPLICATION_FORM_URLENCODED_VALUE.value());
                    }
                }
                connection.sendRequest(request, client.byteBufferClientCallback(reference, latch,
                        ByteBuffer.wrap(body.getBytes(StandardCharsets.UTF_8))));
            }
            latch.await(timeout, TimeUnit.MILLISECONDS);
            ClientResponse response = reference.get();
            if (response != null && response.getResponseCode() == 200) {
                if (body == null || body.isEmpty()) {
                    return response.getAttachment(Http2Client.RESPONSE_BODY);
                } else {
                    ByteBuffer buffer = response.getAttachment(Http2Client.BUFFER_BODY);
                    return new String(buffer.array(), StandardCharsets.UTF_8);
                }
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        } finally {
            client.returnConnection(connection);
        }
        return null;
    }

    /**
     * 文件上传（也支持普通表单）
     * 
     * @param uri     http://localhost:8081
     * @param request new ClientRequest().setMethod(Methods.POST).setPath("/upload")
     * @param body    new FormData()
     * @return
     */
    public static String upload(URI uri, ClientRequest request, FormData body) {
        ClientConnection connection = null;
        try {
            connection = client.borrowConnection(uri, Http2Client.WORKER, client.getDefaultXnioSsl(),
                    Http2Client.BUFFER_POOL, "https".equals(uri.getScheme()) ? httpsOptionMap : OptionMap.EMPTY).get();
            AtomicReference<ClientResponse> reference = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);
            addHostHeader(request);
            if (body == null || !body.iterator().hasNext()) {
                connection.sendRequest(request, client.createClientCallback(reference, latch));
            } else {
                String boundary = "gserver_fileupload";
                request.getRequestHeaders().put(Headers.CONTENT_TYPE, "multipart/form-data; boundary=" + boundary);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                for (String name : body) {
                    for (FormValue formValue : body.get(name)) {
                        if (formValue.isFileItem()) {
                            writeFile(baos, name, formValue, boundary);
                        } else {
                            writeString(baos, name, formValue.getValue(), boundary);
                        }
                    }
                }
                // 文件上传结束标记--boundary--\r\n
                baos.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
                connection.sendRequest(request,
                        client.byteBufferClientCallback(reference, latch, ByteBuffer.wrap(baos.toByteArray())));
            }
            latch.await(timeout, TimeUnit.MILLISECONDS);
            ClientResponse response = reference.get();
            if (response != null && response.getResponseCode() == 200) {
                if (body == null || !body.iterator().hasNext()) {
                    return response.getAttachment(Http2Client.RESPONSE_BODY);
                } else {
                    ByteBuffer buffer = response.getAttachment(Http2Client.BUFFER_BODY);
                    return new String(buffer.array(), StandardCharsets.UTF_8);
                }
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        } finally {
            client.returnConnection(connection);
        }
        return null;
    }

    /**
     * 添加分布式请求头X-Traceability-Id X-Correlation-Id AUTHORIZATION
     */
    public static void propagateHeaders(ClientRequest request, HttpServerExchange exchange) {
        String tid = exchange.getRequestHeaders().getFirst(HttpStringConstants.TRACEABILITY_ID);
        String token = exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION);
        String cid = exchange.getRequestHeaders().getFirst(HttpStringConstants.CORRELATION_ID);
        if (tid != null) {
            request.getRequestHeaders().put(HttpStringConstants.TRACEABILITY_ID, tid);
        }
        if (token != null) {
            request.getRequestHeaders().put(Headers.AUTHORIZATION, token);
        }
        if (cid != null) {
            request.getRequestHeaders().put(HttpStringConstants.CORRELATION_ID, cid);
        }
    }

    static byte[] CRLF = "\r\n".getBytes(StandardCharsets.UTF_8);

    private static void writeFile(ByteArrayOutputStream baos, String name, FormValue file, String boundary)
            throws Exception {
        StringBuilder sb = new StringBuilder("--" + boundary + "\r\n");
        sb.append("Content-Disposition: form-data; name=\"" + name + "\"; filename=\""
                + file.getFileName() + "\"\r\n");
        // Content-Type可选，hutool能根据扩展名匹配正确类型，默认二进制或不传也能成功
        // sb.append("Content-Type: application/octet-stream\r\n");
        baos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        baos.write(CRLF);
        baos.write(NioUtils.toByteArray(file.getFileItem().getInputStream()));
        baos.write(CRLF);
    }

    private static void writeString(ByteArrayOutputStream baos, String name, String value, String boundary)
            throws Exception {
        StringBuilder sb = new StringBuilder("--" + boundary + "\r\n");
        sb.append("Content-Disposition: form-data; name=\"" + name + "\"\r\n");
        baos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        baos.write(CRLF);
        baos.write(value.getBytes());
        baos.write(CRLF);
    }

    private static void addHostHeader(ClientRequest request) {
        if (!request.getRequestHeaders().contains(Headers.HOST)) {
            request.getRequestHeaders().put(Headers.HOST, "localhost");
        }
        if (!request.getRequestHeaders().contains(Headers.TRANSFER_ENCODING)) {
            request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
        }
    }
}
