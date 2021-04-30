package com.xlongwei.light4j.handler;

import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.networknt.handler.LightHttpHandler;
import com.networknt.utility.StringUtils;
import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.PathEndpointSource;
import com.xlongwei.light4j.util.StringUtil;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.extra.template.Template;
import cn.hutool.extra.template.TemplateConfig;
import cn.hutool.extra.template.TemplateConfig.ResourceMode;
import cn.hutool.extra.template.TemplateEngine;
import cn.hutool.extra.template.TemplateUtil;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import lombok.extern.slf4j.Slf4j;

/**
 * demo handler
 * @author xlongwei
 *
 */
@Slf4j
public class DemoHandler implements LightHttpHandler {
	public static final TemplateEngine engine = TemplateUtil.createEngine(new TemplateConfig("beetl/demo", ResourceMode.CLASSPATH));
	private Map<String, AbstractHandler> handlers = new HashMap<>();
	
	public DemoHandler() {
		String pkg = getClass().getPackage().getName()+".demo";
		Set<Class<?>> list = ClassUtil.scanPackageBySuper(pkg, AbstractHandler.class);
		for(Class<?> clazz : list) {
			AbstractHandler handler = (AbstractHandler)ReflectUtil.newInstanceIfPossible(clazz);
			String name = handler.name();
			handlers.put(name, handler);
			log.info("demo {} => {}", name, handler.getClass().getName());
		}
	}

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();
		String service = queryParameters.remove("*").getFirst();
		log.info("{} {}", exchange.getRequestMethod(), exchange.getRequestURI());
		int dot = service.indexOf('.');
		String[] split = StringUtils.split(dot>0 ? service.substring(0, dot) : service, "/");
		String name = split.length>0 ? split[0] : "index", path = split.length>1 ? split[1] : "";
		AbstractHandler handler = handlers.get(name);
		if(handler != null) {
			exchange.putAttachment(AbstractHandler.PATH, path);
			HandlerUtil.parseBody(exchange);
			handler.handleRequest(exchange);
			String accept = exchange.getRequestHeaders().getFirst(Headers.ACCEPT);
			if(accept!=null && accept.startsWith("application/json")) {
				HandlerUtil.sendResp(exchange);
				return;
			}
		}
		exchange.setStatusCode(200);
		exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, "text/html");
		Template template = engine.getTemplate((StringUtil.isBlank(path) ? name : name+"/"+path)+".html");
		String html = template.render((Map<?, ?>)exchange.getAttachment(HandlerUtil.RESP));
		if(StringUtils.isBlank(html)) {
			HandlerUtil.sendResp(exchange);
		}else {
			exchange.removeAttachment(HandlerUtil.RESP);
			exchange.getResponseSender().send(html);
		}
	}

	public static class DemoEndpointSource extends PathEndpointSource {
		public DemoEndpointSource() {
			super("/demo/*");
		}
	}
}
