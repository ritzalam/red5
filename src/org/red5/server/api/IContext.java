package org.red5.server.api;

import org.red5.server.api.persistence.IPersistenceStore;
import org.red5.server.api.service.IServiceInvoker;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * The current context, this object basically wraps the spring context
 * or in the case of the .Net version, any similar system.
 * 
 */
public interface IContext extends ResourcePatternResolver {

	public static final String ID = "red5.context";
	
	public ApplicationContext getApplicationContext();
	
	// public IScopeResolver getScopeResolver();
	public IClientRegistry getClientRegistry();
	public IServiceInvoker getServiceInvoker();
	public IPersistenceStore getPersistanceStore();
	public IScopeHandler lookupScopeHandler(String path);
	public IScope resolveScope(String path);
	public IScope getGlobalScope();
	
	public Object lookupService(String serviceName);
	public Object getBean(String beanId);
	public Object getCoreService(String beanId);

	public IMappingStrategy getMappingStrategy();
}