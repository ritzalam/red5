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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.api.IBandwidthConfigure;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.IContext;
import org.red5.server.api.IFlowControllable;
import org.red5.server.api.IScope;
import org.red5.server.stream.IFlowControlService;

public class Client extends AttributeStore  
	implements IClient {

	protected static Log log =
        LogFactory.getLog(Client.class.getName());
	
	protected String id;
	protected long creationTime;
	protected ClientRegistry registry;
	protected HashMap<IConnection,IScope> connToScope = new HashMap<IConnection,IScope>();
	
	private IBandwidthConfigure bandwidthConfig;
	
	public Client(String id, ClientRegistry registry){
		this.id = id;
		this.registry = registry;
		this.creationTime = System.currentTimeMillis();
	}

	public String getId() {
		return id;
	}

	public long getCreationTime() {
		return creationTime;
	}
	
	public boolean equals(Object obj) {
		if(!(obj instanceof Client)) {
			return false;
		}
		return ((Client) obj).getId().equals(id);
 	}

	//if overriding equals then also do hashCode
	public int hashCode() {
		return id.hashCode();
 	}

	public String toString() {
		return "Client: "+id;
	}
	
	public Set<IConnection> getConnections() {
		return connToScope.keySet();
	}
	
	public Set<IConnection> getConnections(IScope scope) {
		if (scope == null)
			return getConnections();
		
		Set<IConnection> result = new HashSet<IConnection>();
		for (Entry<IConnection, IScope> entry: connToScope.entrySet()) {
			if (scope.equals(entry.getValue()))
				result.add(entry.getKey());
		}
		return result;
	}

	public Collection<IScope> getScopes() {
		return connToScope.values();
	}

	public void disconnect() {
		log.debug("Disconnect, closing "+getConnections().size()+" connections");
		Iterator<IConnection> conns = getConnections().iterator();
		while(conns.hasNext()){
			conns.next().close();
		}
		IContext context = getContextFromConnection();
		if (context == null) return;
		IFlowControlService fcs = (IFlowControlService) context.getBean(
				IFlowControlService.KEY);
		fcs.releaseFlowControllable(this);
	}
		
	public IBandwidthConfigure getBandwidthConfigure() {
		return this.bandwidthConfig;
	}

	public IFlowControllable getParentFlowControllable() {
		// parent is host
		return null;
	}

	public void setBandwidthConfigure(IBandwidthConfigure config) {
		IContext context = getContextFromConnection();
		if (context == null) return;
		IFlowControlService fcs = (IFlowControlService) context.getBean(
				IFlowControlService.KEY);
		this.bandwidthConfig = config;
		fcs.updateBWConfigure(this);
	}

	protected void register(IConnection conn){
		connToScope.put(conn, conn.getScope());
	}
	
	protected void unregister(IConnection conn){
		connToScope.remove(conn);
		if (connToScope.isEmpty()) {
			// This client is not connected to any scopes, remove from registry.
			registry.removeClient(this);
		}
	}
	
	/**
	 * Get the context from anyone of the IConnection.
	 * @return
	 */
	private IContext getContextFromConnection() {
		IConnection conn = null;
		try {
			conn = connToScope.keySet().iterator().next();
		} catch (Exception e) {}
		if (conn != null) {
			return conn.getScope().getContext();
		}
		return null;
	}

}