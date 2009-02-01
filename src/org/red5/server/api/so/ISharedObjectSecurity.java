package org.red5.server.api.so;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2009 by respective authors (see below). All rights reserved.
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

import java.util.List;

import org.red5.server.api.IScope;

/**
 * Interface for handlers that control access to shared objects.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 */
public interface ISharedObjectSecurity {

	/**
	 * Check if the a shared object may be created in the given scope.
	 * 
	 * @param scope scope
	 * @param name name
	 * @param persistent is persistent
	 * @return is creation allowed
	 */
	public boolean isCreationAllowed(IScope scope, String name, boolean persistent);
	
	/**
	 * Check if a connection to the given existing shared object is allowed.
	 * 
	 * @param so shared ojbect
	 * @return is connection alowed
	 */
	public boolean isConnectionAllowed(ISharedObject so);

	/**
	 * Check if a modification is allowed on the given shared object.
	 * 
	 * @param so shared object
	 * @param key key
	 * @param value value
	 * @return true if given key is modifiable; false otherwise
	 */
	public boolean isWriteAllowed(ISharedObject so, String key, Object value);

	/**
	 * Check if the deletion of a property is allowed on the given shared object.
	 * 
	 * @param so shared object
	 * @param key key
	 * @return true if delete allowed; false otherwise
	 */
	public boolean isDeleteAllowed(ISharedObject so, String key);

	/**
	 * Check if sending a message to the shared object is allowed.
	 * 
	 * @param so shared object
	 * @param message message
	 * @param arguments arguments
	 * @return true if allowed
	 */
	public boolean isSendAllowed(ISharedObject so, String message, List<?> arguments);

}
