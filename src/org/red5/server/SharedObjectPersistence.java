package org.red5.server;

import java.util.Iterator;

import org.red5.server.api.SharedObject;
import org.red5.server.context.AppContext;

public interface SharedObjectPersistence {

	/*
	 * Setup application context for this persistene class.
	 */
	void setApplicationContext(AppContext appCtx);
	
	/*
	 * Makes the passed shared object persistent so it can be loaded
	 * through "loadSharedObject" later.
	 */
	void storeSharedObject(SharedObject object);

	/*
	 * Returns the shared object with the given name.
	 * If no SO with the passed name, null is returned.
	 */
	SharedObject loadSharedObject(String name);

	/*
	 * Delete the shared object with the specified name.
	 */
	void deleteSharedObject(String name);

	/*
	 * Returns an iterator over all the shared objects.
	 */
	Iterator getSharedObjects();
	
}
