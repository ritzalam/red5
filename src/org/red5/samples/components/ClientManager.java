package org.red5.samples.components;

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

import java.util.ArrayList;
import java.util.List;

import org.red5.server.api.IScope;
import org.red5.server.api.ScopeUtils;
import org.red5.server.api.so.ISharedObject;
import org.red5.server.api.so.ISharedObjectService;

/**
 * Class that keeps a list of client names in a SharedObject.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class ClientManager {

	/** Name of the shared object attribute that keeps the list of client names. */
	private static String CLIENT_NAMES = "clients";
	
	/** Stores the name of the SharedObject to use. */
	private String name;
	
	/** Should the SharedObject be persistent? */
	private boolean persistent;
	
	/**
	 * Create a new instance of the client manager.
	 * 
	 * @param name
	 * 			name of the shared object to use
	 * @param persistent
	 * 			should the shared object be persistent
	 */
	public ClientManager(String name, boolean persistent) {
		this.name = name;
		this.persistent = persistent;
	}
	
	/**
	 * Return the shared object to use for the given scope.
	 * 
	 * @param scope
	 * 			the scope to return the shared object for
	 * @return the shared object to use
	 */
	private ISharedObject getSharedObject(IScope scope) {
		ISharedObjectService service = (ISharedObjectService) ScopeUtils.getScopeService(scope, ISharedObjectService.SHARED_OBJECT_SERVICE);
		return service.getSharedObject(scope, name, persistent);
	}
	
	/**
	 * A new client connected. This adds the username to
	 * the shared object of the passed scope. 
	 * 
	 * @param scope
	 * 			scope the client connected to 
	 * @param username
	 * 			name of the user that connected
	 */
	@SuppressWarnings("unchecked")
	public void addClient(IScope scope, String username) {
		ISharedObject so = getSharedObject(scope);
		so.beginUpdate();
		List<String> clientNames = (List<String>) so.getAttribute(CLIENT_NAMES, new ArrayList<String>());
		clientNames.add(username);
		so.setAttribute(CLIENT_NAMES, clientNames);
		so.endUpdate();
	}

	/**
	 * A client disconnected. This removes the username from
	 * the shared object of the passed scope.
	 * 
	 * @param scope
	 * 			scope the client disconnected from
	 * @param username
	 * 			name of the user that disconnected
	 */
	@SuppressWarnings("unchecked")
	public void removeClient(IScope scope, String username) {
		ISharedObject so = getSharedObject(scope);
		if (!so.hasAttribute(CLIENT_NAMES))
			// SharedObject is empty, this shouldn't happen.
			return;
		
		so.beginUpdate();
		List<String> clientNames = (List<String>) so.getAttribute(CLIENT_NAMES);
		if (clientNames.contains(username)) {
			// Remove client and update SO.
			clientNames.remove(username);
			so.setAttribute(CLIENT_NAMES, clientNames);
		}
		so.endUpdate();
	}
	
}
