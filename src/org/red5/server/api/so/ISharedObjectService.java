package org.red5.server.api.so;

import java.util.Iterator;

public interface ISharedObjectService {

	/**
	 * Get a set of the shared object names
	 * 
	 * @return set containing the shared object names
	 */
	public Iterator<String> getSharedObjectNames();

	/**
	 * Create a new shared object
	 * 
	 * @param name
	 *            the name of the shared object
	 * @param persistent
	 *            will the shared object be persistent
	 * @return true if the shared object was created, otherwise false
	 */
	public boolean createSharedObject(String name, boolean persistent);

	/**
	 * Get a shared object by name
	 * 
	 * @param name
	 *            the name of the shared object
	 * @return shared object, or null if not found
	 */
	public ISharedObject getSharedObject(String name);

	public boolean hasSharedObject(String name);

	
}
