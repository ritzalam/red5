package org.red5.server.api.so;

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

import java.util.Set;

import org.red5.server.api.IScope;

/**
 * Service that manages shared objects.
 * 
 */
public interface ISharedObjectService {

	public final static String SHARED_OBJECT_SERVICE = "sharedObjectService";
	
	/**
	 * Get a set of the shared object names.
	 * 
	 * @param scope
	 * 			the scope to return the shared object names from
	 * @return set containing the shared object names
	 */
	public Set<String> getSharedObjectNames(IScope scope);

	/**
	 * Create a new shared object.
	 * 
	 * @param scope
	 * 			the scope to create the shared object in
	 * @param name
	 *          the name of the shared object
	 * @param persistent
	 *          will the shared object be persistent
	 * @return <code>true</code> if the shared object was created, otherwise <code>false</code>
	 */
	public boolean createSharedObject(IScope scope, String name, boolean persistent);

	/**
	 * Get a shared object by name.
	 * 
	 * @param scope
	 * 			the scope to get the shared object from
	 * @param name
	 * 			the name of the shared object
	 * @return shared object, or <code>null</code> if not found
	 */
	public ISharedObject getSharedObject(IScope scope, String name);

	/**
	 * Get a shared object by name and create it if it doesn't exist.
	 * 
	 * @param scope
	 * 			the scope to get the shared object from
	 * @param name
	 * 			the name of the shared object
	 * @param persistent
	 * 			should the shared object be created persistent 
	 * @return the shared object
	 */
	public ISharedObject getSharedObject(IScope scope, String name, boolean persistent);

	/**
	 * Check if a shared object exists.
	 * 
	 * @param scope
	 * 			the scope to check for the shared object
	 * @param name
	 * 			the name of the shared object
	 * @return <code>true</code> if the shared object exists, otherwise <code>false</code>
	 */
	public boolean hasSharedObject(IScope scope, String name);
	
}
