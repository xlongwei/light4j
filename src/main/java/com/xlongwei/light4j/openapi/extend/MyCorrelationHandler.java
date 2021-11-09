package com.xlongwei.light4j.openapi.extend;

import com.networknt.config.Config;
import com.networknt.correlation.CorrelationHandler;
import com.networknt.handler.Handler;
import com.networknt.httpstring.HttpStringConstants;
import com.networknt.utility.ModuleRegistry;
import com.networknt.utility.StringUtils;
import com.networknt.utility.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import io.undertow.server.HttpServerExchange;

public class MyCorrelationHandler extends DummyMiddlewareHandler {
    private static final Logger logger = LoggerFactory.getLogger(MyCorrelationHandler.class);
    private static final String CID = "cId";
    private static final String CONFIG_NAME = "correlation";

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        // check if the cid is in the request header
        String cId = exchange.getRequestHeaders().getFirst(HttpStringConstants.CORRELATION_ID);
        if (cId == null) {
            // if not set, check the autgen flag and generate if set to true
            String tId = exchange.getRequestHeaders().getFirst(HttpStringConstants.TRACEABILITY_ID);
            if (StringUtils.isBlank(tId)) {// showapi转发请求头有bug
                tId = exchange.getRequestHeaders().getFirst("showapi_res_id");
                exchange.getResponseHeaders().put(HttpStringConstants.TRACEABILITY_ID, tId);
            }
            if (CorrelationHandler.config.isAutogenCorrelationID()) {
                // generate a UUID and put it into the request header
                cId = Util.getUUID();
            } else {
                cId = tId;
            }
            if (tId != null) {
                exchange.getRequestHeaders().put(HttpStringConstants.CORRELATION_ID, cId);
                if (!tId.equals(cId) && logger.isInfoEnabled()) {
                    logger.info("Associate traceability Id " + tId + " with correlation Id " + cId);
                }
            }
        }
        // Add the cId into MDC so that all log statement will have cId as part of it.
        MDC.put(CID, cId);
        // logger.debug("Init cId:" + cId);
        Handler.next(exchange, next);
    }

    @Override
    public boolean isEnabled() {
        return CorrelationHandler.config.isEnabled();
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(CorrelationHandler.class.getName(),
                Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME), null);
    }
}
