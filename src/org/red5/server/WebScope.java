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

/**
 *  Web scope is special scope that is aware of servlet context and
 * represents scope of Red5 application in servlet container (or application server) like Tomcat, Jetty or JBoss.
 *
 * Web scope is aware of virtual hosts configuration for Red5 application and is the first scope that instantiated
 * after Red5 application got started.
 * Then it loads virtual hosts configuration, adds mappings of paths to global scope that is injected thru Spring
 * IoC context file and runs initialization process.
 *
 * Red5 server implementation instance and ServletContext are injected as well.
 */
public class WebScope extends Scope implements ServletContextAware {

    /**
     * Logger
     */
    protected static Log log = LogFactory.getLog(WebScope.class.getName());
    /**
     * Server instance
     */
	protected IServer server;
    /**
     * Servlet context
     */
	protected ServletContext servletContext;
    /**
     * Context path
     */
	protected String contextPath;
    /**
     * Virtual hosts list as string
     */
	protected String virtualHosts;
    /**
     * Hostnames
     */
	protected String[] hostnames;

    /**
     * Setter for global scope. Sets persistence class.
     *
     * @param globalScope       Red5 global scope
     */
	public void setGlobalScope(IGlobalScope globalScope) {
		// XXX: this is called from nowhere, remove?
		super.setParent(globalScope);
		try {
			setPersistenceClass(globalScope.getStore().getClass().getName());
		} catch (Exception error) {
			log.error("Could not set persistence class.", error);
		}
	}

    /**
     * Web scope has no name
     */
	public void setName() {
		throw new RuntimeException("Cannot set name, you must set context path");
	}

    /**
     * Can't set parent to Web scope. Web scope is top level.
     */
	public void setParent() {
		throw new RuntimeException(
				"Cannot set parent, you must set global scope");
	}

    /**
     * Setter for server
     * @param server            Server instance
     */
	public void setServer(IServer server) {
		this.server = server;
	}

    /**
     * Servlet context
     * @param servletContext     Servlet context
     */
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

    /**
     * Setter for context path
     * @param contextPath     Context path
     */
	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
		super.setName(contextPath.substring(1));
	}

    /**
     * Setter for virtual hosts. Creates array of hostnames.
     * @param virtualHosts           Virtual hosts list as string
     */
	public void setVirtualHosts(String virtualHosts) {
		this.virtualHosts = virtualHosts;
        // Split string into array of vhosts
        hostnames = virtualHosts.split(",");
		for (int i = 0; i < hostnames.length; i++) {
			hostnames[i] = hostnames[i].trim();
			if (hostnames[i].equals("*")) {
				hostnames[i] = "";
			}
		}
	}

    /**
     *  Map all vhosts to global scope then initialize
     */
	public void register() {
		if (hostnames != null && hostnames.length > 0) {
			for (String element : hostnames) {
				server.addMapping(element, getName(), getParent().getName());
			}
		}
		init();
	}

}
