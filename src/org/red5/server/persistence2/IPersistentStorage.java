package org.red5.server.persistence2;

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

import java.io.IOException;
import java.util.Iterator;

import org.red5.server.zcontext.AppContext;

/**
 * Storage for persistent objects. 
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 */

public interface IPersistentStorage {

	/**
	 * Setup the application context for the storage.
	 * 
	 * @param appCtx
	 * 		the application context to use
	 */
	public void setApplicationContext(AppContext appCtx);
	
	/**
	 * Generate a new unique object id.
	 * 
	 * @return
	 * 		the generated object id
	 */
	public String newPersistentId();
	
	/**
	 * Make the passed object persistent.
	 *  
	 * @param object
	 * 		the object to store
	 */
	public void storeObject(IPersistable object) throws IOException;
	
	/**
	 * Load a persistent object with the given name.
	 * 
	 * @param name
	 * 		the name of the object to load
	 * @return
	 * 		the loaded object or <code>null</code> if no such object was found
	 */
	public IPersistable loadObject(String name) throws IOException;
	
	/**
	 * Delete the persistent object with the given name.
	 *  
	 * @param name
	 * 		the name of the object to delete
	 */
	public void removeObject(String name) throws IOException;
	
	/**
	 * Return iterator over all objects in the storage.
	 * 
	 * @return
	 * 		iterator over all objects
	 */
	public Iterator getObjects();
}
