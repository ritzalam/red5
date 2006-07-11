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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.api.IBasicScope;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.IScope;
import org.red5.server.api.event.IEvent;

public abstract class BaseConnection extends AttributeStore 
	implements IConnection {
	
	protected static Log log =
        LogFactory.getLog(BaseConnection.class.getName());
	
	protected String type;
	protected String host;
	protected String remoteAddress;
	protected int remotePort;
	protected String path;
	protected String sessionId;
	protected long readMessages = 0;
	protected long writtenMessages = 0;
	protected long droppedMessages = 0;
	protected Map<String,String> params = null;
	
	protected IClient client = null;
	protected Scope scope = null;
	protected Set<IBasicScope> basicScopes;
	
	public BaseConnection(String type, String host, String remoteAddress, int remotePort, String path, String sessionId, Map<String,String> params){
		this.type = type;
		this.host = host;
		this.remoteAddress = remoteAddress;
		this.remotePort = remotePort;
		this.path = path;
		this.sessionId = sessionId;
		this.params = params;
		this.basicScopes = new HashSet<IBasicScope>();
	}
	
	public void initialize(IClient client){
		if (this.client != null && this.client instanceof Client) {
			// Unregister old client
			((Client) this.client).unregister(this);
		}
		this.client = client;
		if (this.client instanceof Client) {
			// Register new client
			((Client) this.client).register(this);
		}
	}
	
	public String getType(){
		return type;
	}

	public String getHost() {
		return host;
	}

	public String getRemoteAddress() {
		return remoteAddress;
	}

	public int getRemotePort() {
		return remotePort;
	}
	
	public String getPath(){
		return path;
	}

	public String getSessionId() {
		return sessionId;
	}

	public Map<String,String> getParams(){
		return params;
	}

	public IClient getClient() {
		return client;
	}

	public boolean isConnected(){
		return scope != null;
	}

	public boolean connect(IScope newScope) {
		return connect(newScope, null);
	}

	public boolean connect(IScope newScope, Object[] params) {
		final Scope oldScope = scope;
		scope = (Scope) newScope;
		if (scope.connect(this, params)){
			if(oldScope != null) 
				oldScope.disconnect(this);
			return true;
		} else 	{
			scope = oldScope;
			return false;
		}
	}

	public IScope getScope() {
		return scope;
	}
	
	public void close(){
	
		if(scope != null) {
			
			log.debug("Close, disconnect from scope, and children");
			try {
				Set<IBasicScope> tmpScopes = new HashSet<IBasicScope>(basicScopes);
				for(IBasicScope basicScope : tmpScopes){
					unregisterBasicScope(basicScope);
				}
			} catch (Exception err) {
				log.error("Error while unregistering basic scopes.", err);
			}
			
			try {
				scope.disconnect(this);
			} catch (Exception err) {
				log.error("Error while disconnecting from scope " + scope, err);
			}
			
			if (client != null && client instanceof Client) {
				((Client) client).unregister(this);
				client = null;
			}
			
			scope=null;
		} else {
			log.debug("Close, not connected nothing to do.");
		}
		
	}
	
	public void notifyEvent(IEvent event) {
		// TODO Auto-generated method stub		
	}

	public void dispatchEvent(IEvent event) {
		
	}

	public boolean handleEvent(IEvent event) {
		return getScope().handleEvent(event);
	}

	public Iterator<IBasicScope> getBasicScopes() {
		return basicScopes.iterator();
	}
	
	public void registerBasicScope(IBasicScope basicScope){
		basicScopes.add(basicScope);
		basicScope.addEventListener(this);
	}
	
	public void unregisterBasicScope(IBasicScope basicScope){
		basicScopes.remove(basicScope);
		basicScope.removeEventListener(this);
	}

	public abstract long getReadBytes();
	public abstract long getWrittenBytes();
	
	public long getReadMessages() {
		return readMessages;
	}
	
	public long getWrittenMessages() {
		return writtenMessages;
	}
	
	public long getDroppedMessages() {
		return droppedMessages;
	}
	
	/* This is really a utility
	public boolean switchScope(String contextPath) {
		// At the moment this method is not dealing with tree schematics
		Scope newScope = (Scope) ScopeUtils.resolveScope(scope, contextPath);
		if(newScope == null) return false;
		return connect(scope);
	}
	*/
	
}