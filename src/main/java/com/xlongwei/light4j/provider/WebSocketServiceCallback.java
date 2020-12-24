package com.xlongwei.light4j.provider;

import java.util.HashMap;
import java.util.Map;

import org.xnio.IoUtils;

import com.alibaba.fastjson.JSONObject;
import com.networknt.utility.StringUtils;
import com.xlongwei.light4j.handler.ServiceHandler;
import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.JsonUtil;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.websockets.core.WebSocketChannel;
import jodd.util.StringUtil;

/**
 * 服务调用
 * @author xlongwei
 *
 */
public class WebSocketServiceCallback extends WebSocketAbstractCallback {

	@Override
	protected void onTextMessage(WebSocketChannel channel, String text) {
		JSONObject request = JsonUtil.parseNew(text);//{handler,path,data:{},sequence}
		String handler = request.getString("handler"), path = request.getString("path");
		if(!StringUtil.isBlank(handler)) {
			AbstractHandler service = ServiceHandler.handlers.get(handler);
			if(service != null) {
				HttpServerExchange exchange = new HttpServerExchange(null);
				exchange.getRequestHeaders().add(Headers.X_FORWARDED_FOR, getClientIp(channel));
				exchange.setSourceAddress(channel.getSourceAddress());
				boolean ipsConfig = HandlerUtil.ipsConfig(exchange, handler);
				ServiceHandler.serviceCount(handler);
				if(!ipsConfig) {
					sendText(channel, "{\"error\":\"access is limited\"}");
					IoUtils.safeClose(channel);
					return;
				}
				exchange.putAttachment(AbstractHandler.PATH, StringUtils.isBlank(path)?"":path);
				if(request.containsKey("data")) {
					//{"handler":"datetime","path":"info","data":{"day":"2020-12-17"}}
					JSONObject data = request.getJSONObject("data");
					exchange.putAttachment(HandlerUtil.BODY, data);
				}
				try{
					service.handleRequest(exchange);
					Object resp = exchange.removeAttachment(HandlerUtil.RESP);
					if(resp != null) {
						Map<String, Object> map = new HashMap<>(4);
						if(request.containsKey("sequence")) {
							map.put("sequence", request.getString("sequence"));
						}
						map.put("data", resp);
						sendText(channel, JSONObject.toJSONString(map));
					}else {
						sendText(channel, ServiceHandler.BAD_REQUEST);
					}
				}catch(Exception e) {
					sendText(channel, "{\"error\":\""+e.getMessage()+"\"}");
				}
			}
		}
	}

}
