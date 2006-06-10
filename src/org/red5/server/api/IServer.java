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
import java.util.Map;

/**
 * The interface that represents the Red5 server.
 *  
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard (luke@codegent.com)
 *
 */
public interface IServer {

	public static final String ID = "red5.server";
	
	/**
	 * Get the global scope with a given name.
	 * 
	 * @param name
	 * 			name of the global scope
	 * @return the global scope
	 */
	public IGlobalScope getGlobal(String name);
	
	/**
	 * Register a global scope.
	 * 
	 * @param scope
	 * 			the global scope to register
	 */
	public void registerGlobal(IGlobalScope scope);
	
	/**
	 * Lookup the global scope for a host.
	 * 
	 * @param hostName
	 * 			the name of the host
	 * @param contextPath
	 * 			the path in the host 
	 * @return the found global scope or <code>null</code>
	 */
	public IGlobalScope lookupGlobal(String hostName, String contextPath);
	
	/**
	 * Map a virtual hostname and a path to the name of a global scope.
	 * 
	 * @param hostName
	 * 			the name of the host to map
	 * @param contextPath
	 * 			the path to map
	 * @param globalName
	 * 			the name of the global scope to map to
	 * @return <code>true</code> if the name was mapped, otherwise <code>false</code>
	 */
	public boolean addMapping(String hostName, String contextPath, String globalName);
	
	/**
	 * Unregister a previously mapped global scope. 
	 *  
	 * @param hostName
	 * 			the name of the host to unmap
	 * @param contextPath
	 * 			the path for this host to unmap
	 * @return <code>true</code> if the global scope was unmapped, otherwise <code>false</code>
	 */
	public boolean removeMapping(String hostName, String contextPath);
	
	/**
	 * Query informations about the global scope mappings.
	 *  
	 * @return a map containing informations about the mappings
	 */
	public Map<String,String> getMappingTable();
	
	/**
	 * Get list of global scope names.
	 * 
	 * @return names of global scopes
	 */
	public Iterator<String> getGlobalNames();
	
	/**
	 * Get list of global scopes.
	 * 
	 * @return list of global scopes
	 */
	public Iterator<IGlobalScope> getGlobalScopes();
	
}