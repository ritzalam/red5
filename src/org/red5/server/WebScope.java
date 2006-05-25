package org.red5.server;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.api.IGlobalScope;
import org.red5.server.api.IServer;
import org.springframework.web.context.ServletContextAware;

public class WebScope extends Scope implements ServletContextAware {

	// Initialize Logging
	protected static Log log =
        LogFactory.getLog(WebScope.class.getName());
		
	protected IServer server;
	protected ServletContext servletContext;
	protected String contextPath;
	protected String virtualHosts;
	protected String[] hostnames;
	
	public void setGlobalScope(IGlobalScope globalScope){
		// XXX: this is called from nowhere, remove?
		super.setParent(globalScope);
		try {
			setPersistenceClass(globalScope.getStore().getClass().getName());
		} catch (Exception error) {
			log.error("Could not set persistence class.", error);
		}
	}

	public void setName(){
		throw new RuntimeException("Cannot set name, you must set context path");
	}
	
	public void setParent(){
		throw new RuntimeException("Cannot set parent, you must set global scope");
	}
	
	public void setServer(IServer server) {
		this.server = server;
	}

	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}
	
	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
		super.setName(contextPath.substring(1));
	}

	public void setVirtualHosts(String virtualHosts) {
		this.virtualHosts = virtualHosts;
		hostnames = virtualHosts.split(",");
		for (int i = 0; i < hostnames.length; i++) {
			hostnames[i] = hostnames[i].trim();
			if(hostnames[i].equals("*")){
				hostnames[i] = "";
			}
		}
	}

	public void register(){
		if(hostnames != null && hostnames.length > 0){
			for (int i = 0; i < hostnames.length; i++) {
				server.addMapping(hostnames[i], getName(),((IGlobalScope) getParent()).getName());
			}
		} 
		init();
	}

}
