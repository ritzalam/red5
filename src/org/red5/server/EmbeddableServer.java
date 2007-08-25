package org.red5.server;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2007 by respective authors (see below). All rights reserved.
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

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.api.IGlobalScope;
import org.red5.server.api.IServer;
import org.red5.server.api.listeners.IConnectionListener;
import org.red5.server.api.listeners.IScopeListener;

/**
 * Red5 server core class implementation for use in J2EE archive deployments
 * (war, ear, sar etc).
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class EmbeddableServer {

	// Initialize Logging
	protected static Log log = LogFactory.getLog(EmbeddableServer.class
			.getName());

	private IServer instance;

	public void setServer(IServer server) {
		instance = server;
	}

	public IServer getServer() {
		return instance;
	}

	private void findInstance() {
		try {
			instance = (IServer) Naming
					.lookup("rmi://localhost:1099/red5Server");
		} catch (Exception e) {
			log.warn("Red5 server registry lookup error", e);
		}
		instance = new Server();
	}

	public IGlobalScope lookupGlobal(String hostName, String contextPath)
			throws java.rmi.RemoteException {
		return instance.lookupGlobal(hostName, contextPath);
	}

	public IGlobalScope getGlobal(String name) throws java.rmi.RemoteException {
		return instance.getGlobal(name);
	}

	public void registerGlobal(IGlobalScope scope) {
		try {
			log.info("Registering global scope: " + scope.getName());
			if (instance == null) {
				findInstance();
			}
			instance.registerGlobal(scope);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean addMapping(String hostName, String contextPath,
			String globalName) throws java.rmi.RemoteException {
		log.info("Add mapping global: " + globalName + " host: " + hostName
				+ " context: " + contextPath);
		return instance.addMapping(hostName, contextPath, globalName);
	}

	public boolean removeMapping(String hostName, String contextPath)
			throws java.rmi.RemoteException {
		log.info("Remove mapping host: " + hostName + " context: "
				+ contextPath);
		return instance.removeMapping(hostName, contextPath);
	}

	public Map<String, String> getMappingTable()
			throws java.rmi.RemoteException {
		return instance.getMappingTable();
	}

	public Iterator<String> getGlobalNames() throws java.rmi.RemoteException {
		return instance.getGlobalNames();
	}

	public Iterator<IGlobalScope> getGlobalScopes()
			throws java.rmi.RemoteException {
		return instance.getGlobalScopes();
	}

	public void addListener(IScopeListener listener) {
		try {
			instance.addListener(listener);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void addListener(IConnectionListener listener) {
		try {
			instance.addListener(listener);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void removeListener(IScopeListener listener) {
		try {
			instance.removeListener(listener);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void removeListener(IConnectionListener listener) {
		try {
			instance.removeListener(listener);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
