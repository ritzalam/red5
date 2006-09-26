package org.red5.server.net.rtmpt;

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

import java.util.Map;

import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.Server;
import org.apache.catalina.Wrapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.TomcatLoader;
import org.red5.server.api.IServer;
import org.springframework.context.ApplicationContextAware;

/**
 * Loader for the RTMPT server which uses Tomcat.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class TomcatRTMPTLoader extends TomcatLoader implements
		ApplicationContextAware {

	// Initialize Logging
	protected static Log log = LogFactory.getLog(TomcatRTMPTLoader.class
			.getName());

	protected Server rtmptServer;

	protected IServer server;

	protected RTMPTHandler handler;

	private Host host;

	private Context context;

	public void setServer(IServer server) {
		log.debug("RTMPT setServer");
		this.server = server;
	}

	public void setRTMPTHandler(RTMPTHandler handler) {
		log.debug("RTMPT setRTMPTHandler");
		this.handler = handler;
	}

	@Override
	public void init() {
		log.info("Loading RTMPT context");

		try {
			getApplicationContext();
		} catch (Exception e) {
			log.error("Error loading tomcat configuration", e);
		}

		// embedded.setRealm(realm);

		host.addChild(context);

		log.debug("Null check - engine: " + (null == engine) + " host: "
				+ (null == host));
		engine.addChild(host);

		// add new Engine to set of Engine for embedded server
		embedded.addEngine(engine);

		// add new Connector to set of Connectors for embedded server,
		// associated with Engine
		embedded.addConnector(connector);

		// start server
		try {
			log.info("Starting RTMPT engine");
			embedded.start();
		} catch (org.apache.catalina.LifecycleException e) {
			log.error("Error loading tomcat", e);
		}

	}

	/**
	 * Set a host
	 * 
	 * @param host
	 */
	public void setHost(Host host) {
		log.debug("RTMPT setHost");
		this.host = host;
	}

	/**
	 * Set primary context
	 * 
	 * @param context
	 */
	//public void setContext(Context context) {
	//	log.debug("RTMPT setContext");
	//	this.context = context;
	//}
	public void setContext(Map<String, String> contextMap) {
		log.debug("RTMPT setContext (map)");
		context = embedded.createContext(contextMap.get("path"), contextMap
				.get("docBase"));
		context.setReloadable(false);
	}

	/**
	 * Set primary wrapper / servlet
	 * 
	 * @param wrapper
	 */
	public void setWrapper(Wrapper wrapper) {
		log.debug("RTMPT setWrapper");
		context.addChild(wrapper);
	}

	/**
	 * Set servlet mappings
	 * 
	 * @param mappings
	 */
	public void setMappings(Map<String, String> mappings) {
		log.debug("Servlet mappings: " + mappings.size());
		for (String key : mappings.keySet()) {
			context.addServletMapping(mappings.get(key), key);
		}
	}

}
