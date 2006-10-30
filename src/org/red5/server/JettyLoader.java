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

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.webapp.WebAppContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class JettyLoader implements ApplicationContextAware {

	// Initialize Logging
	protected static Log log = LogFactory.getLog(JettyLoader.class.getName());

	protected String jettyConfig = "classpath:/jetty.xml";
	protected String defaultWebConfig = "web-default.xml";

	protected Server jetty;
	
	// used during context creation
	private static String appRoot;	

	// We store the application context in a ThreadLocal so we can access it
	// from "org.red5.server.jetty.Red5WebPropertiesConfiguration" later.
	private static ThreadLocal<ApplicationContext> applicationContext = new ThreadLocal<ApplicationContext>();

	public void setApplicationContext(ApplicationContext context)
			throws BeansException {
		applicationContext.set(context);
	}

	public static ApplicationContext getApplicationContext() {
		return applicationContext.get();
	}

	public void init() {
		// So this class is left just starting jetty
		try {
			log.info("Loading jetty6 context from: " + jettyConfig);
			XmlBeanFactory bf = new XmlBeanFactory(getApplicationContext().getResource(jettyConfig));
			Server jetty = (Server) bf.getBean("Server");

			// root location for servlet container
			String serverRoot = System.getProperty("red5.root");
			log.debug("Server root: " + serverRoot);
			// set in the system for tomcat classes
			System.setProperty("jetty.home", serverRoot);
			System.setProperty("jetty.class.path", serverRoot + "/lib");

			log.info("Starting jetty servlet engine");
			
			String webAppRoot = System.getProperty("red5.webapp.root");
			String[] handlersArr = new String[]{"org.mortbay.jetty.webapp.WebInfConfiguration", 
					"org.mortbay.jetty.webapp.WebXmlConfiguration", 
					"org.mortbay.jetty.webapp.JettyWebXmlConfiguration", 
					"org.mortbay.jetty.webapp.TagLibConfiguration", 
					"org.red5.server.jetty.Red5WebPropertiesConfiguration"};
				
			//root context
			WebAppContext ctx = new WebAppContext();
			ctx.setContextPath("/");
			ctx.setWar(webAppRoot + "root");

			HandlerCollection handlers = new HandlerCollection();
			handlers.setHandlers(new Handler[]{ctx, new DefaultHandler()});
			jetty.setHandler(handlers);
			
			try {
				WebAppContext.addWebApplications(jetty, webAppRoot, defaultWebConfig, handlersArr, true, true);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			jetty.start();
			
		} catch (Exception e) {
			log.error("Error loading jetty", e);
		}

	}
	
}
