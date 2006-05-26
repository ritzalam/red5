package org.red5.server.api.persistence;

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

/**
 * Storage for persistent objects. 
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard (luke@codegent.com)
 * @author Joachim Bauch (jojo@struktur.de)
 */

public interface IPersistenceStore {
	
	/**
	 * Make the passed object persistent.
	 *  
	 * @param obj
	 * 		the object to store
	 */
	public boolean save(IPersistable obj);
	
	/**
	 * Load a persistent object with the given name.  The object must provide
	 * either a constructor that takes an input stream as only parameter or an
	 * empty constructor so it can be loaded from the persistence store.
	 * 
	 * @param name
	 * 		the name of the object to load
	 * @return
	 * 		the loaded object or <code>null</code> if no such object was found
	 */
	public IPersistable load(String name);

	/**
	 * Load state of an already instantiated persistent object.
	 * 
	 * @param obj
	 * 		the object to initializ
	 * @return
	 * 		true if the object was initialized, false otherwise
	 */
	public boolean load(IPersistable obj);

	/**
	 * Delete the passed persistent object.
	 *  
	 * @param obj
	 * 		the object to delete
	 */
	public boolean remove(IPersistable obj);
	
	/**
	 * Delete the persistent object with the given name.
	 *  
	 * @param name
	 * 		the name of the object to delete
	 */
	public boolean remove(String name);
	
	/**
	 * Return iterator over the names of all already loaded objects in the storage.
	 * 
	 * @return
	 * 		iterator over all objects
	 */
	public Iterator<String> getObjectNames();
	
	/**
	 * Return iterator over the already loaded objects in the storage.
	 * 
	 * @return
	 * 		iterator over all objects
	 */
	public Iterator<IPersistable> getObjects();

}
