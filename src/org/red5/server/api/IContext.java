package org.red5.server.api;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006 by respective authors (see below). All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either version 2.1 of the License, or (at your option) any later 
 * version. 
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this library; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 */

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
	/**
	 * Get client registry. Client registry is a place where all clients are
	 * registred.
	 * 
	 * @return	Client registry object
	 */
	public IClientRegistry getClientRegistry();
	
	/**
	 * Returns service invoker object. Service invokers are objects that make
	 * service calls to client side NetConnection objects.
	 * 
	 * @return		Service invoker object
	 */
	public IServiceInvoker getServiceInvoker();
	
	/**
	 * Returns persistence store object, a storage for persistent objects like
	 * persistent SharedObjects.
	 * 
	 * @return	persistence store object
	 */
	public IPersistenceStore getPersistanceStore();
	
	/**
	 * Returns scope handler (object that handler all actions related to the
	 * scope) by path. See {@link IScopeHandler} for details.
	 * 
	 * @param path
	 *            Path of scope handler
	 * @return		Scope handler
	 */
	public IScopeHandler lookupScopeHandler(String path);
	
	/**
	 * Returns scope by path. You can think of IScope as of tree items, used to
	 * separate context and resources between users. See {@link IScope} for more
	 * details.
	 * 
	 * @param path
	 *            Path of scope
	 * @return		IScope object
	 */
	public IScope resolveScope(String path);
	
	/**
	 * Returns global scope reference
	 * 
	 * @return	global scope reference
	 */
	public IScope getGlobalScope();
	
	/**
	 * Returns service by name. 
	 * 
	 * @param serviceName
	 *            Name of service
	 * @return				Service object
	 */
	public Object lookupService(String serviceName);
	
	/**
	 * Returns bean by ID
	 * 
	 * @param beanId
	 *            Bean ID
	 * @return			Given bean instance
	 */
	public Object getBean(String beanId);
	
	/**
	 * Returns core service by bean id
	 * 
	 * @param beanId
	 *            Bean ID
	 * @return			Core service
	 */
	public Object getCoreService(String beanId);

	/**
	 * Returns IMappingStrategy object
	 * 
	 * @return	IMappingStrategy object
	 */	
	public IMappingStrategy getMappingStrategy();
}