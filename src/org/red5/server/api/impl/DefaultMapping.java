package org.red5.server.api.impl;

import org.red5.server.api.Mapping;
import org.red5.server.api.ScopeHandler;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/** 
 * Provides mapping between context paths and scope handlers
 * Provides mapping between service names and service beans
 */
public class DefaultMapping implements Mapping, ApplicationContextAware {
	
	public ApplicationContext context;
	public String appScopeHandlerName = "appScopeHandler";
	public String scopeHandlerSuffix = "ScopeHandler";
	public String serviceSuffix = "Service";
	
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.context = context;
	}

	public void setAppScopeHandlerName(String appScopeHandlerName) {
		this.appScopeHandlerName = appScopeHandlerName;
	}

	public void setScopeHandlerSuffix(String scopeHandlerSuffix) {
		this.scopeHandlerSuffix = scopeHandlerSuffix;
	}

	public void setServiceSuffix(String serviceSuffix) {
		this.serviceSuffix = serviceSuffix;
	}

	public ScopeHandler mapContextPathToScopeHandler(String contextPath) {
		ScopeHandler handler = null;
		if(contextPath == null || contextPath.equals("") ){
			handler = (ScopeHandler) context.getBean(appScopeHandlerName);
		} else {
			if(contextPath.indexOf("/") != -1){
				contextPath = contextPath.substring(0, contextPath.indexOf("/"));
			}
			handler = (ScopeHandler) context.getBean(contextPath+scopeHandlerSuffix);
			if(handler == null){
				handler =  (ScopeHandler) context.getBean(scopeHandlerSuffix);
			}
		}
		return handler;
	}

	public Object mapServiceNameToService(String serviceName) {
		return context.getBean(serviceName + serviceSuffix);
	}

}