package com.xlongwei.light4j.util;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.networknt.client.Http2Client;

import org.xnio.OptionMap;

import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Http2Util {
    /** 请求超时（毫秒） */
    public static long timeout = 30000;
    private static Http2Client client = Http2Client.getInstance();

    public static String execute(URI host, ClientRequest clientRequest, String body) {
        try (ClientConnection connection = client
                .borrowConnection(host, Http2Client.WORKER, null, Http2Client.BUFFER_POOL, OptionMap.EMPTY).get()) {
            AtomicReference<ClientResponse> responseReference = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);
            connection.sendRequest(clientRequest,
                    body == null || body.isEmpty() ? client.createClientCallback(responseReference, latch)
                            : client.createClientCallback(responseReference, latch, body));
            latch.await(timeout, TimeUnit.MILLISECONDS);
            ClientResponse clientResponse = responseReference.get();
            client.returnConnection(connection);
            if (clientResponse != null && clientResponse.getResponseCode() == 200) {
                return clientResponse.getAttachment(Http2Client.RESPONSE_BODY);
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
        return "{}";
    }
}
