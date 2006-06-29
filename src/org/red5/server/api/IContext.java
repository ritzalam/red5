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