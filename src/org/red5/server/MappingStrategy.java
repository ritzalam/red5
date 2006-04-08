package org.red5.server;

import org.red5.server.api.IMappingStrategy;

public class MappingStrategy implements IMappingStrategy {

	private static final String ROOT = "";
	private static final String HANDLER = "Handler";
	private static final String DIR = "/";
	private static final String SERVICE = ".service";
	private String defaultApp = "default";
	
	public void setDefaultApp(String defaultApp) {
		this.defaultApp = defaultApp;
	}

	public String mapResourcePrefix(String path) {
		if(path == null || path.equals(ROOT)) return defaultApp + DIR;
		else return path + DIR;
	}

	public String mapScopeHandlerName(String path) {
		if(path == null || path.equals(ROOT)) return defaultApp + HANDLER;
		else return path + HANDLER;
	}

	public String mapServiceName(String name) {
		return  name + SERVICE;
	}
	
}
