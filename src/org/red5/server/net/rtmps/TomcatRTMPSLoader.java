package org.red5.server.net.rtmps;

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
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Loader;
import org.apache.catalina.Server;
import org.apache.catalina.Valve;
import org.apache.catalina.core.AprLifecycleListener;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.loader.WebappLoader;
import org.red5.server.api.IServer;
import org.red5.server.net.rtmpt.TomcatRTMPTLoader;
import org.red5.server.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loader for the RTMPS server which uses Tomcat.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class TomcatRTMPSLoader extends TomcatRTMPTLoader {

	// Initialize Logging
	private static Logger log = LoggerFactory.getLogger(TomcatRTMPSLoader.class);
	
	/**
	 * RTMPS Tomcat engine.
	 */
	protected Engine rtmpsEngine;	
	
	/**
	 * Setter for server
	 * 
	 * @param server
	 *            Value to set for property 'server'.
	 */
	public void setServer(IServer server) {
		log.debug("RTMPS setServer");
		this.server = server;
	}

	/** {@inheritDoc} */
	@Override
	public void init() {
		log.info("Loading RTMPS context");
		
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();

		rtmpsEngine = embedded.createEngine();
		rtmpsEngine.setDefaultHost(host.getName());
		rtmpsEngine.setName("red5RTMPSEngine");
		rtmpsEngine.setParentClassLoader(classloader);
		
		host.setParentClassLoader(classloader);		

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
		if (ldr != null) {
			if (ldr instanceof WebappLoader) {
				log.debug("Replacing context loader");				
				((WebappLoader) ldr).setLoaderClass("org.red5.server.tomcat.WebappClassLoader");
			} else {
				log.debug("Context loader was instance of {}", ldr.getClass().getName());
			}
		} else {
			log.debug("Context loader was null");
			WebappLoader wldr = new WebappLoader(classloader);
			wldr.setLoaderClass("org.red5.server.tomcat.WebappClassLoader");
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
				
		// add the host
		rtmpsEngine.addChild(host);

		// add new Engine to set of Engine for embedded server
		embedded.addEngine(rtmpsEngine);

		//turn off native apr support
		AprLifecycleListener listener = new AprLifecycleListener();
		listener.setSSLEngine("off");
		connector.addLifecycleListener(listener);
		
		log.debug("Protocol handler: {}", connector.getProtocolHandlerClassName());

		// set connection properties
		connector.setSecure(true);
        connector.setScheme("https");

		// set other connection / protocol handler properties
		for (String key : connectionProperties.keySet()) {
			log.debug("Setting connection property: {} = {}", key, connectionProperties.get(key));
			connector.setProperty(key, connectionProperties.get(key));
		}		
		
		// add new Connector to set of Connectors for embedded server,
		// associated with Engine
		embedded.addConnector(connector);

		// start server
		try {
			log.info("Starting RTMPS engine");
			//embedded.start();
			connector.start();
		} catch (Exception e) {
			log.error("Error loading tomcat", e);
		} finally {
			registerJMX();		
		}

	}
	
}
