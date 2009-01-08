package org.red5.server.net.rtmpt;

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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Valve;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.loader.WebappLoader;
import org.red5.server.ServletClassLoader;
import org.red5.server.api.IServer;
import org.red5.server.tomcat.TomcatLoader;
import org.red5.server.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loader for the RTMPT server which uses Tomcat.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class TomcatRTMPTLoader extends TomcatLoader {

	// Initialize Logging
	private static Logger log = LoggerFactory.getLogger(TomcatRTMPTLoader.class);

	/**
	 * RTMPT Tomcat engine.
	 */
	protected Engine rtmptEngine;	
	
	/**
	 * Server instance
	 */
	protected IServer server;

	/**
	 * Context, in terms of JEE context is web application in a servlet
	 * container
	 */
	protected Context context;

	/**
	 * Extra servlet mappings to add
	 */
	protected Map<String, String> servletMappings = new HashMap<String, String>();
	
	
	/**
	 * Setter for server
	 * 
	 * @param server
	 *            Value to set for property 'server'.
	 */
	public void setServer(IServer server) {
		log.debug("RTMPT setServer");
		this.server = server;
	}

	/** {@inheritDoc} */
	@Override
	public void init() {
		log.info("Loading RTMPT context");

		rtmptEngine = embedded.createEngine();
		rtmptEngine.setDefaultHost(host.getName());
		rtmptEngine.setName("red5RTMPTEngine");
		
		// add the valves to the host
		for (Valve valve : valves) {
			log.debug("Adding host valve: {}", valve);
			((StandardHost) host).addValve(valve);
		}				
		
		// create and add root context
		File appDirBase = new File(webappFolder);
		String webappContextDir = FileUtil.formatPath(appDirBase.getAbsolutePath(), "/root");
		Context ctx = embedded.createContext("/", webappContextDir);
		ctx.setReloadable(false);
		log.debug("Context name: {}", ctx.getName());
		Object ldr = ctx.getLoader();
		log.debug("Context loader: {}", ldr);		
		if (ldr == null) {
			log.debug("Context loader was null");
			ClassLoader classloader;
			try {
				classloader = ServletClassLoader.getServletClassLoader(new File(webappContextDir), ServletClassLoader.USE_WAR_LIB);
			} catch (IOException e) {
				log.warn("Servlet class loader setup error", e);
				classloader = Thread.currentThread().getContextClassLoader();
			}
			log.debug("Context class loader: {}", classloader);
			WebappLoader wldr = new WebappLoader(classloader);
			ctx.setLoader(wldr);
		}  
		appDirBase = null;
		webappContextDir = null;
		
		host.addChild(ctx);
		
		// add servlet wrapper
		StandardWrapper wrapper = new StandardWrapper();
		wrapper.setServletName("RTMPTServlet");
		wrapper.setServletClass("org.red5.server.net.servlet.RTMPTServlet");
		ctx.addChild(wrapper);
		
		// add servlet mappings
		ctx.addServletMapping("/open/*", "RTMPTServlet");
		ctx.addServletMapping("/close/*", "RTMPTServlet");
		ctx.addServletMapping("/send/*", "RTMPTServlet");
		ctx.addServletMapping("/idle/*", "RTMPTServlet");	
		
		// add any additional mappings
		for (String key : servletMappings.keySet()) {
			context.addServletMapping(servletMappings.get(key), key);
		}		
		
		rtmptEngine.addChild(host);

		// add new Engine to set of Engine for embedded server
		embedded.addEngine(rtmptEngine);

		// set connection properties
		for (String key : connectionProperties.keySet()) {
			log.debug("Setting connection property: {} = {}", key, connectionProperties.get(key));
			connector.setProperty(key, connectionProperties.get(key));
		}		
		
		// start server
		try {
    		// add new Connector to set of Connectors for embedded server,
    		// associated with Engine
    		embedded.addConnector(connector);

			log.info("Starting RTMPT engine");
			//embedded.start();
			connector.start();
		} catch (Exception e) {
			log.error("Error loading tomcat", e);
		} finally {
			registerJMX();		
		}

	}
	
	/**
	 * Set servlet mappings
	 * 
	 * @param mappings
	 */
	public void setMappings(Map<String, String> mappings) {
		log.debug("Servlet mappings: {}", mappings.size());
		servletMappings.putAll(mappings);
	}
	
}
