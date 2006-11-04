package org.red5.server;

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

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.api.IGlobalScope;
import org.red5.server.api.IServer;
import org.springframework.web.context.ServletContextAware;

public class WebScope extends Scope implements ServletContextAware {

	// Initialize Logging
	protected static Log log = LogFactory.getLog(WebScope.class.getName());

	protected IServer server;

	protected ServletContext servletContext;

	protected String contextPath;

	protected String virtualHosts;

	protected String[] hostnames;

	public void setGlobalScope(IGlobalScope globalScope) {
		// XXX: this is called from nowhere, remove?
		super.setParent(globalScope);
		try {
			setPersistenceClass(globalScope.getStore().getClass().getName());
		} catch (Exception error) {
			log.error("Could not set persistence class.", error);
		}
	}

	public void setName() {
		throw new RuntimeException("Cannot set name, you must set context path");
	}

	public void setParent() {
		throw new RuntimeException(
				"Cannot set parent, you must set global scope");
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
			if (hostnames[i].equals('*')) {
				hostnames[i] = "";
			}
		}
	}

	public void register() {
		if (hostnames != null && hostnames.length > 0) {
			for (String element : hostnames) {
				server.addMapping(element, getName(),
						((IGlobalScope) getParent()).getName());
			}
		}
		init();
	}

}
