package com.xlongwei.light4j.openapi.extend;

import static com.networknt.cors.CorsHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS;
import static com.networknt.cors.CorsHeaders.ACCESS_CONTROL_ALLOW_HEADERS;
import static com.networknt.cors.CorsHeaders.ACCESS_CONTROL_ALLOW_METHODS;
import static com.networknt.cors.CorsHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static com.networknt.cors.CorsHeaders.ACCESS_CONTROL_MAX_AGE;
import static com.networknt.cors.CorsHeaders.ACCESS_CONTROL_REQUEST_HEADERS;
import static com.networknt.cors.CorsUtil.isPreflightedRequest;
import static io.undertow.server.handlers.ResponseCodeHandler.HANDLE_200;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import com.networknt.cors.CorsUtil;
import com.xlongwei.light4j.util.HandlerUtil;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;

/**
 * my cors handler
 * @author xlongwei
 *
 */
public class MyCorsHttpHandler extends DummyMiddlewareHandler {
	private static final Collection<String> ALLOWED_METHODS = Arrays.asList("GET", "POST");
	private static final long ONE_HOUR_IN_SECONDS = TimeUnit.HOURS.toSeconds(1);

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
        HeaderMap headers = exchange.getRequestHeaders();
        if (CorsUtil.isCoreRequest(headers)) {
            if (isPreflightedRequest(exchange)) {
            	setCorsResponseHeaders(exchange);
                HANDLE_200.handleRequest(exchange);
                return;
            }
            setCorsResponseHeaders(exchange);
        }
        HandlerUtil.requestStartTime(exchange);
        super.handleRequest(exchange);
	}

    private void setCorsResponseHeaders(HttpServerExchange exchange) throws Exception {
    	HeaderMap requestHeaders = exchange.getRequestHeaders();
        HeaderMap responseHeaders = exchange.getResponseHeaders();
        if (requestHeaders.contains(Headers.ORIGIN)) {
        	// responseHeaders.add(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
//            if(CorsUtil.matchOrigin(exchange, allowedOrigins) != null) {
               exchange.getResponseHeaders().addAll(ACCESS_CONTROL_ALLOW_ORIGIN, requestHeaders.get(Headers.ORIGIN));
//                exchange.getResponseHeaders().add(Headers.VARY, Headers.ORIGIN_STRING);
//            }
        }
        responseHeaders.addAll(ACCESS_CONTROL_ALLOW_METHODS, ALLOWED_METHODS);
        HeaderValues requestedHeaders = requestHeaders.get(ACCESS_CONTROL_REQUEST_HEADERS);
        if (requestedHeaders != null && !requestedHeaders.isEmpty()) {
            responseHeaders.addAll(ACCESS_CONTROL_ALLOW_HEADERS, requestedHeaders);
        } else {
            responseHeaders.add(ACCESS_CONTROL_ALLOW_HEADERS, Headers.CONTENT_TYPE_STRING);
            responseHeaders.add(ACCESS_CONTROL_ALLOW_HEADERS, Headers.WWW_AUTHENTICATE_STRING);
            responseHeaders.add(ACCESS_CONTROL_ALLOW_HEADERS, Headers.AUTHORIZATION_STRING);
        }
        responseHeaders.add(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        responseHeaders.add(ACCESS_CONTROL_MAX_AGE, ONE_HOUR_IN_SECONDS);
    }
}
