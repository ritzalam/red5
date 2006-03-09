package org.red5.server;

import java.util.Iterator;

import org.red5.server.api.SharedObject;
import org.red5.server.context.AppContext;

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
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 */

/**
 * Persistence for shared objects.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 */

public interface SharedObjectPersistence {

	/**
	 * Setup application context for this persistence class.
	 * 
	 * @param	appCtx	the application context to use for this persistence
	 */
	void setApplicationContext(AppContext appCtx);
	
	/**
	 * Makes the passed shared object persistent so it can be loaded
	 * through {@link #loadSharedObject loadSharedObject} later.
	 * 
	 * @param	object	the shared object to store
	 */
	void storeSharedObject(SharedObject object);

	/**
	 * Returns the shared object with the given name.
	 * 
	 * @param	name	the name of the shared object to load
	 * @return	the requested object or <code>null</code> if no such shared object exists
	 */
	SharedObject loadSharedObject(String name);

	/**
	 * Delete the shared object with the specified name.
	 * 
	 * @param	name	the name of the shared object to delete
	 */
	void deleteSharedObject(String name);

	/**
	 * Enumerate all shared objects.
	 * 
	 * @return an <code>Iterator</code> over all the shared objects
	 */
	Iterator getSharedObjects();
	
}
