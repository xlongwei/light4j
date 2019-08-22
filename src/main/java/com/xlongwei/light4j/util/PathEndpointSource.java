package com.xlongwei.light4j.util;

import java.util.ArrayList;
import java.util.List;

import com.networknt.handler.config.EndpointSource;
import com.networknt.utility.Util;

/**
 * 让路径支持所有http方法
 * @author xlongwei
 *
 */
public class PathEndpointSource implements EndpointSource {
	private String path;
	public PathEndpointSource(String path) {
		this.path = path;
	}

	@Override
	public Iterable<Endpoint> listEndpoints() {
		List<Endpoint> endpoints = new ArrayList<>();
		for(String method : Util.METHODS) {
			Endpoint endpoint = new Endpoint(path, method);
			endpoints.add(endpoint);
		}
		return endpoints;
	}

}
