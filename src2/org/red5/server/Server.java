package org.red5.server;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright © 2006 by respective authors (see below). All rights reserved.
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
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.net.NetworkManager;
import org.springframework.context.ApplicationContext;

/**
 * The Server class is composed of a NetworkManager {@link org.red5.server.net.NetworkManager.java}
 * Additionally, the server calls the provided networkManager's up() method which enables the
 * Red5 server to receive client connections. 
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 * @version 0.3
 */
public class Server {
	
	// NOTE: Moved main to Standalone.java
	// Initialize Logging
	protected static Log log =
        LogFactory.getLog(Server.class.getName());
	
	protected NetworkManager networkManager;
	protected SessionRegistry sessionRegistry;
	protected ApplicationContext serviceContext;
	
	/**
	 *	Starts the Red5 server by calling networkManager.up() 
	 *  Once the server is up, clients can connect connect using
	 *  rtmp://hostname[:port]/
	 */
	public void startup(){
		if(log.isInfoEnabled()){
			log.info("Startup");
			networkManager.up();
		}
	}

	/**
	 * Get the networkManager instance
	 * @return NetworkManager
	 */
	public NetworkManager getNetworkManager() {
		return networkManager;
	}

	/**
	 * Sets the networkManager instance
	 * @param networkManager
	 */
	public void setNetworkManager(NetworkManager networkManager) {
		this.networkManager = networkManager;
	}

	/**
	 * Gets the Spring application context
	 * @return ApplicationContext
	 */
	public ApplicationContext getServiceContext() {
		return serviceContext;
	}

	/**
	 * Sets the Spring application context
	 * @param serviceContext
	 */
	public void setServiceContext(ApplicationContext serviceContext) {
		this.serviceContext = serviceContext;
	}

	/**
	 * Gets the session registry
	 * @return SessionRegistry
	 */
	public SessionRegistry getSessionRegistry() {
		return sessionRegistry;
	}

	/**
	 * Sets the session registry
	 * @param sessionRegistry
	 */
	public void setSessionRegistry(SessionRegistry sessionRegistry) {
		this.sessionRegistry = sessionRegistry;
	}

}
