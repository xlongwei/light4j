package com.xlongwei.light4j.openapi;

import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.networknt.handler.LightHttpHandler;
import com.networknt.utility.StringUtils;
import com.xlongwei.light4j.handler.ServiceHandler;
import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.PathEndpointSource;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;
import io.undertow.server.HttpServerExchange;
import lombok.extern.slf4j.Slf4j;

/**
 * openapi handler
 * @author xlongwei
 *
 */
@Slf4j
public class OpenapiHandler implements LightHttpHandler {
	private Map<String, AbstractHandler> handlers = new HashMap<>();
	
	public OpenapiHandler() {
		String service = getClass().getPackage().getName()+".handler";
		Set<Class<?>> list = ClassUtil.scanPackageBySuper(service, AbstractHandler.class);
		for(Class<?> clazz : list) {
			AbstractHandler handler = (AbstractHandler)ReflectUtil.newInstanceIfPossible(clazz);
			String name = handler.name();
			handlers.put(name, handler);
			log.info("openapi {} => {}", name, handler.getClass().getName());
		}
	}

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }
		Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();
		String service = queryParameters.remove("*").getFirst();
		log.info("{} {}", exchange.getRequestMethod(), exchange.getRequestURI());
		int dot = service.indexOf('.');
		String[] split = StringUtils.split(dot>0 ? service.substring(0, dot) : service, "/");
		if(split.length > 0) {
			String name = split[0];
			AbstractHandler handler = handlers.get(name);
			if(handler == null) {
				handler = ServiceHandler.handlers.get(name);
				ServiceHandler.serviceCount(name);
			}
			if(handler != null) {
				String path = split.length>1 ? split[1] : "";
				exchange.putAttachment(AbstractHandler.PATH, path);
				HandlerUtil.parseBody(exchange);
				handler.handleRequest(exchange);
			}
		}
		HandlerUtil.sendResp(exchange);
	}
	public static class OpenapiEndpointSource extends PathEndpointSource {
		public OpenapiEndpointSource() {
			super("/openapi/*");
		}
	}
}
