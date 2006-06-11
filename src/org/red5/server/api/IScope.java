package org.red5.server.api;

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
 */

import java.util.Iterator;
import java.util.Set;

import org.red5.server.api.service.IServiceHandlerProvider;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * The scope object.
 * 
 * A statefull object shared between a group of clients connected to the same
 * context path. Scopes are arranged in a hierarchical way, so its possible for
 * a scope to have a parent. If a client is connect to a scope then they are also
 * connected to its parent scope. The scope object is used to access resources,
 * shared object, streams, etc.
 * 
 * The following are all names for scopes: application, room, place, lobby.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard (luke@codegent.com)
 */
public interface IScope extends IBasicScope, ResourcePatternResolver, IServiceHandlerProvider {

	public static final String ID = "red5.scope";
	
	public static final String TYPE = "scope";
	public static final String SEPARATOR = ":";
	
	/**
	 * Check to see if this scope has a child scope matching a given name.
	 * 
	 * @param name
	 *            the name of the child scope
	 * @return <code>true</code> if a child scope exists, otherwise <code>false</code>
	 */
	public boolean hasChildScope(String name);
	
	public boolean hasChildScope(String type, String name);
	
	public boolean createChildScope(String name);

	public boolean addChildScope(IBasicScope scope);
	
	public void removeChildScope(IBasicScope scope);
	
	/**
	 * Get a set of the child scope names.
	 * 
	 * @return set containing child scope names
	 */
	public Iterator<String> getScopeNames();
	
	public Iterator<String> getBasicScopeNames(String type);

	/**
	 * Get a child scope by name.
	 * 
	 * @param name
	 *            name of the child scope
	 * @return the child scope, or null if no scope is found
	 */
	public IBasicScope getBasicScope(String type, String name);

	
	public IScope getScope(String name);
	
	
	/**
	 * Get a set of connected clients.  You can get the connections by passing the
	 * scope to the clients <code>getConnections</code> method.
	 * 
	 * @return set containing all connected clients
	 * @see org.red5.server.api.IClient#getConnections(IScope)
	 */
	public Set<IClient> getClients();
		
	/**
	 * Get a connection iterator.  You can call remove, and the connection will be closed.
	 * @return iterator holding all connections
	 */
	public Iterator<IConnection> getConnections();
		
	/**
	 * Lookup connections.
	 * 
	 * @param	client object
	 * @return set of connection objects (readonly)
	 */
	public Set<IConnection> lookupConnections(IClient client);

	public IContext getContext();
	
	/* ----------------------------------------------- */ 
	
	public boolean hasHandler(); 
	public IScopeHandler getHandler(); 
	
	public String getContextPath();
	
	public boolean connect(IConnection conn);
	public boolean connect(IConnection conn, Object[] params);
	public void disconnect(IConnection conn);
	
	
}