package org.red5.server.api;

import java.util.Map;
import java.util.List;

import org.red5.server.context.Client;

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
 * Serverside access to shared objects. Changes to the shared objects are
 * propagated to all subscribed clients.
 * 
 * If you want to modify multiple attributes and notify the clients about all
 * changes at once, you can use code like this:
 * <p>
 * <code>
 * SharedObject.beginUpdate();<br />
 * SharedObject.updateAttribute("One", "1");<br />
 * SharedObject.updateAttribute("Two", "2");<br />
 * SharedObject.deleteAttribute("Three");<br />
 * SharedObject.endUpdate();<br />
 * </code>
 * </p>
 * 
 * All changes between "beginUpdate" and "endUpdate" will be sent to the clients
 * using one notification event.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 */

public interface SharedObject {

	/**
	 * Returns the name of the shared object.
	 * 
	 * @return the name of the shared object
	 */
	public String getName();

	/**
	 * Returns the version of the shared object. The version is incremented
	 * automatically on each modification.
	 * 
	 * @return the version of the shared object
	 */
	public int getVersion();

	/**
	 * Check if the object has been created as persistent shared object by the
	 * client.
	 * 
	 * @return true if the shared object is persistent, false otherwise
	 */
	public boolean isPersistent();

	/**
	 * Return a map containing all attributes of the shared object. <br />
	 * NOTE: modifications to this map are _not_ propagated to the connected
	 * clients.
	 * 
	 * @return a map containing all attributes of the shared object
	 */
	public Map getData();

	/**
	 * Add / edit an attribute.
	 * 
	 * @param name
	 *            the name of the attribute to change
	 * @param value
	 *            the new value of the attribute
	 * @return true if the attribute value has changed, false otherwise
	 */
	public boolean updateAttribute(String name, Object value);

	/**
	 * Return the value for a given attribute.
	 * 
	 * @param name
	 *            the name of the attribute to get
	 * @return the attribute value or null if the attribute doesn't exist
	 */
	public Object getAttribute(String name);

	/**
	 * Remove an attribute from the shared object.
	 * 
	 * @param name
	 *            the name of the attribute to delete
	 * @return true if the attribute has been found and deleted, false otherwise
	 */
	public boolean deleteAttribute(String name);

	/**
	 * Send a message to a handler of the shared object.
	 * 
	 * @param handler
	 *            the name of the handler to call
	 * @param arguments
	 *            a list of objects that should be passed as arguments to the
	 *            handler
	 */
	public void sendMessage(String handler, List arguments);

	/**
	 * Delete all attributes of the shared object.
	 */
	public void clear();

	/**
	 * Start performing multiple updates to the shared object.
	 */
	public void beginUpdate();

	/**
	 * The multiple updates are complete, notify clients about all changes at
	 * once.
	 */
	public void endUpdate();

	/**
	 * Register a new client with the channel that should receive updates of the
	 * shared object.
	 * 
	 * @param client
	 *            the client that wants to subscribe to this object
	 * @param channel
	 *            the connection channel of this client
	 */
	public void registerClient(Client client, int channel);

	/**
	 * Unregister all channels of a client.
	 * 
	 * @param client
	 *            the client to unsubscribe from this object
	 */
	public void unregisterClient(Client client);

	/**
	 * Unregister one channel of a client.
	 * 
	 * @param client
	 *            the client to unsubscribe a channel from this object
	 * @param channel
	 *            the connection channel to unsubscribe
	 */
	public void unregisterClient(Client client, int channel);
}