package org.red5.server;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 *
 * Copyright (c) 2006-2008 by respective authors (see below). All rights reserved.
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

import java.io.IOException;

import org.red5.server.api.IClientRegistry;
import org.red5.server.api.IMappingStrategy;
import org.red5.server.api.IScope;
import org.red5.server.api.IScopeHandler;
import org.red5.server.api.persistence.IPersistenceStore;
import org.red5.server.api.service.IServiceInvoker;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

/**
 * {@inheritDoc}
 *
 * <p>This is basic context implementation used by Red5.</p>
 */
public interface ContextMBean {

	public IScope getGlobalScope();

	public IScope resolveScope(String path);

	public IScope resolveScope(IScope root, String path);

	public IPersistenceStore getPersistanceStore();

	public ApplicationContext getApplicationContext();

	public void setContextPath(String contextPath);

	public IClientRegistry getClientRegistry();

	public IScope getScope();

	public IServiceInvoker getServiceInvoker();

	public Object lookupService(String serviceName);

	public IScopeHandler lookupScopeHandler(String contextPath);

	public IMappingStrategy getMappingStrategy();

	public Resource[] getResources(String pattern) throws IOException;

	public Resource getResource(String path);

	public IScope resolveScope(String host, String path);

	public Object getBean(String beanId);

	public Object getCoreService(String beanId);

}
