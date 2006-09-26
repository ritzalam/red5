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

import java.util.List;
import java.util.Map;

import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Realm;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.startup.Embedded;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class TomcatLoader implements ApplicationContextAware {
	// Initialize Logging
	protected static Log log = LogFactory.getLog(TomcatLoader.class.getName());

	protected Embedded embedded;

	protected Engine engine;

	protected Realm realm;

	protected Connector connector;

	private Host baseHost;

	// We store the application context in a ThreadLocal so we can access it
	// later.
	protected static ThreadLocal<ApplicationContext> applicationContext = new ThreadLocal<ApplicationContext>();

	// used during context creation
	private static String appRoot;

	static {
		log.info("Init tomcat");
		// root location for servlet container
		String serverRoot = System.getProperty("red5.root");
		log.info("Server root: " + serverRoot);
		// root location for servlet container
		appRoot = serverRoot + "/webapps";
		log.info("Application root: " + appRoot);
		// set in the system for tomcat classes
		System.setProperty("catalina.home", serverRoot);
	}

	public void setApplicationContext(ApplicationContext context)
			throws BeansException {
		applicationContext.set(context);
	}

	public static ApplicationContext getApplicationContext() {
		return applicationContext.get();
	}

	public void init() {
		log.info("Loading tomcat context");

		try {
			getApplicationContext();
		} catch (Exception e) {
			log.error("Error loading tomcat configuration", e);
		}

		embedded.setRealm(realm);

		// baseHost = embedded.createHost(hostName, appRoot);
		engine.addChild(baseHost);

		// add new Engine to set of Engine for embedded server
		embedded.addEngine(engine);

		// add new Connector to set of Connectors for embedded server,
		// associated with Engine
		embedded.addConnector(connector);

		// start server
		try {
			log.info("Starting tomcat servlet engine");
			embedded.start();
		} catch (org.apache.catalina.LifecycleException e) {
			log.error("Error loading tomcat", e);
		}
	}

	public Realm getRealm() {
		return realm;
	}

	public void setRealm(Realm realm) {
		log.info("Setting realm: " + realm.getClass().getName());
		this.realm = realm;
	}

	public Engine getEngine() {
		return engine;
	}

	public void setEngine(Engine engine) {
		log.info("Setting engine: " + engine.getClass().getName());
		this.engine = engine;
	}

	public Host getBaseHost() {
		return baseHost;
	}

	public void setBaseHost(Host baseHost) {
		log.debug("setBaseHost");
		this.baseHost = baseHost;
	}

	public Embedded getEmbedded() {
		return embedded;
	}

	public void setEmbedded(Embedded embedded) {
		log.info("Setting embedded: " + embedded.getClass().getName());
		this.embedded = embedded;
	}

	public Connector getConnector() {
		return connector;
	}

	public void setConnector(Connector connector) {
		log.info("Setting connector: " + connector.getClass().getName());
		this.connector = connector;
	}

	/**
	 * Set additional connectors
	 * 
	 * @param connectors
	 */
	public void setConnectors(List<Connector> connectors) {
		log.debug("setConnectors: " + connectors.size());
		for (Connector ctr : connectors) {
			embedded.addConnector(ctr);
		}
	}

	/**
	 * Set additional hosts
	 * 
	 * @param hosts
	 */
	public void setHosts(List<Host> hosts) {
		log.debug("setHosts: " + hosts.size());
		for (Host host : hosts) {
			engine.addChild(host);
		}
	}

	/**
	 * Set additional valves
	 * 
	 * @param valves
	 */
	public void setValves(List<Valve> valves) {
		log.debug("setValves: " + valves.size());
		for (Valve valve : valves) {
			((StandardHost) baseHost).addValve(valve);
		}
	}

	/**
	 * Set additional contexts
	 * 
	 * @param contexts
	 */
	public void setContexts(Map<String, String> contexts) {
		log.debug("setContexts: " + contexts.size());
		for (String key : contexts.keySet()) {
			baseHost.addChild(embedded.createContext(key, appRoot
					+ contexts.get(key)));
		}
	}

	public org.apache.catalina.Context addContext(String path, String docBase) {
		org.apache.catalina.Context c = embedded.createContext(path, docBase);
		baseHost.addChild(c);
		return c;
	}

	public void shutdown() {
		log.info("Shutting down tomcat context");
		try {
			embedded.stop();
		} catch (Exception e) {
			log.warn("Tomcat could not be stopped", e);
		}
	}

}