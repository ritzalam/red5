package org.red5.server.api.persistence;

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

import java.io.IOException;

import org.red5.io.object.Input;
import org.red5.io.object.Output;

/**
 * Base interface for objects that can be made persistent.
 * 
 * Every object that complies to this interface must provide either a constructor
 * that takes an input stream as only parameter or an empty constructor so it can
 * be loaded from the persistence store.
 * 
 * However this is not required for objects that are created by the application
 * and initialized afterwards.
 * 
 * @see org.red5.io.object.Input
 * @see IPersistenceStore#load(String)
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard (luke@codegent.com)
 * @author Joachim Bauch (jojo@struktur.de)
 */

public interface IPersistable {
	
	/**
	 * Prefix for attribute names that should not be made persistent.
	 */
	public static final String TRANSIENT_PREFIX = "_transient";
	
	/**
	 * Returns <code>true</code> if the object is persistent, <code>false</code>
	 * otherwise.
	 * 
	 * @return
	 * 		true or false
	 */
	public boolean isPersistent();

	/**
	 * Set the persistent flag of the object.
	 * 
	 * @param persistent
	 * 		true if the object is persistent, false otherwise
	 */
	public void setPersistent(boolean persistent);

	/**
	 * Returns the name of the persistent object.
	 * 
	 * @return
	 * 		the name of the object
	 */
	public String getName();
	
	/**
	 * Set the name of the persistent object.
	 * 
	 * @param name
	 * 		the new name of the object
	 */
	public void setName(String name);
	
	/**
	 * Returns the type of the persistent object.
	 * 
	 * @return
	 * 		the type of the object
	 */
	public String getType();
	
	/**
	 * Returns the path of the persistent object.
	 * 
	 * @return
	 * 		the path of the object
	 */
	public String getPath();

	/**
	 * Set the path of the persistent object.
	 * 
	 * @param path
	 * 		the new path of the object
	 */
	public void setPath(String path);
	
	/**
	 * Returns the timestamp when the object was last modified.
	 * 
	 * @return
	 * 		last modification date in milliseconds
	 */
	public long getLastModified();

	/**
	 * Returns the persistence store this object is stored in
	 * 
	 * @return
	 * 		the store for this object
	 */
	public IPersistenceStore getStore();
	
	/**
	 * Store a reference to the persistence store in the object.
	 * 
	 * @param store
	 * 		the store the object is saved in
	 */
	void setStore(IPersistenceStore store);
	
	/**
	 * Write the object to the passed output stream.
	 * 
	 * @param output
	 * 		the output stream to write to
	 */
	void serialize(Output output) throws IOException;
	
	/**
	 * Load the object from the passed input stream.
	 * 
	 * @param input
	 * 		the input stream to load from
	 */
	void deserialize(Input input) throws IOException;
	
}
