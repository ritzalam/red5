package org.red5.server.api;

import java.util.HashMap;
import java.util.List;

import org.red5.server.context.Client;

/**
 * Serverside access to shared objects.  Changes to the SO are propagated to
 * all connected clients.
 * 
 * If you want to modify multiple attributes and notify the clients about all
 * changes at once, you can use code like this: 
 * 
 * SharedObject.beginUpdate();
 * SharedObject.updateAttribute("One", "1");
 * SharedObject.updateAttribute("Two", "2");
 * SharedObject.deleteAttribute("Three");
 * SharedObject.endUpdate();
 * 
 * All changes between "beginUpdate" and "endUpdate" will be sent to the clients
 * using one notification event.
 */

public interface SharedObject {
	
	/*
	 * Returns the name of the shared object.
	 */
	public String getName();
	
	/*
	 * Returns the version of the shared object.  The version
	 * is incremented automatically on each modification.
	 */
	public int getVersion();
	
	/*
	 * Returns true if the shared object has been created as
	 * persistent SO by the client.
	 */
	public boolean isPersistent();
	
	/*
	 * Return a map containing all attributes of the shared
	 * object.
	 * 
	 * NOTE: modifications to this map are _not_ propagated to
	 *       the connected clients.
	 */
	public HashMap getData();
	
	/*
	 * Add / edit an attribute.  Returns true if the attribute
	 * value has changed.
	 */
	public boolean updateAttribute(String name, Object value);
	
	/*
	 * Return the value for a given attribute.  Return null
	 * if the attribute doesn't exist.
	 */
	public Object getAttribute(String name);
	
	/*
	 * Remove an attribute from the SO.  Returns true if the
	 * attribute has been found and deleted.
	 */
	public boolean deleteAttribute(String name);

	/*
	 * Send a message to a handler of the shared object.
	 */
	public void sendMessage(String handler, List arguments);
	
	/*
	 * Delete all attributes of the shared object.
	 */
	public void clear();
	
	/*
	 * Start performing multiple updates to the shared object.
	 */
	public void beginUpdate();
	
	/*
	 * The multiple updates are complete, notify clients about all
	 * changes at once.
	 */
	public void endUpdate();
	
	/*
	 * Register a new client with the channel that should receive
	 * updates of the SO.
	 */
	public void registerClient(Client client, int channel);
	
	/*
	 * Unregister all channels of a client.
	 */
	public void unregisterClient(Client client);
	
	/*
	 * Unregister one channel of a client.
	 */
	public void unregisterClient(Client client, int channel);
}
