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

/**
 * Use to Map between names and objects
 * 
 * Provides mapping for:
 * <ul>
 * <li>context path --&gt scope handler</li>
 * <li>service name --&gt service object</li>
 * </ul>
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard (luke@codegent.com)
 */
public interface Mapping {

	/**
	 * Maps between a contextPath and a scope handler object
	 * 
	 * @param contextPath
	 *            the name of the context path eg: /myapp/room
	 * @return scope handler object, or null if no handler was found
	 */
	public ScopeHandler mapContextPathToScopeHandler(String contextPath);

	/**
	 * Maps between a service name and a service object
	 * 
	 * @param serviceName
	 *            the name of the service
	 * @return the service object, or null if no service was found
	 */
	public Object mapServiceNameToService(String serviceName);

}