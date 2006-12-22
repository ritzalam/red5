package org.red5.server.so;

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

import static org.red5.server.api.so.ISharedObject.TYPE;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.io.object.Deserializer;
import org.red5.io.object.Input;
import org.red5.io.object.Output;
import org.red5.io.object.Serializer;
import org.red5.server.api.IAttributeStore;
import org.red5.server.api.event.IEventListener;
import org.red5.server.api.persistence.IPersistable;
import org.red5.server.api.persistence.IPersistenceStore;
import org.red5.server.net.rtmp.Channel;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.so.ISharedObjectEvent.Type;

public class SharedObject implements IPersistable, Constants {

	protected static Log log = LogFactory.getLog(SharedObject.class.getName());

	protected String name = "";

	protected String path = "";

	// true if the SharedObject was stored by the persistence framework 
	protected boolean persistent = false;

	// true if the client / server created the SO to be persistent
	protected boolean persistentSO = false;

	protected IPersistenceStore storage = null;

	protected int version = 1;

	protected Map<String, Object> data = null;

	protected Map<String, Integer> hashes = new HashMap<String, Integer>();

	protected int updateCounter = 0;

	protected boolean modified = false;

	protected long lastModified = -1;

	protected SharedObjectMessage ownerMessage;

	protected LinkedList<ISharedObjectEvent> syncEvents = new LinkedList<ISharedObjectEvent>();

	protected HashSet<IEventListener> listeners = new HashSet<IEventListener>();

	protected IEventListener source = null;

	/** Constructs a new SharedObject. */
    public SharedObject() {
		// This is used by the persistence framework
		data = new ConcurrentHashMap<String, Object>();

		ownerMessage = new SharedObjectMessage(null, null, -1, false);
	}

	public SharedObject(Input input) throws IOException {
		this();
		deserialize(input);
	}

	public SharedObject(Map<String, Object> data, String name, String path,
			boolean persistent) {
		this.data = new ConcurrentHashMap<String, Object>();
		this.data.putAll(data);
		this.name = name;
		this.path = path;
		this.persistentSO = persistent;

		ownerMessage = new SharedObjectMessage(null, name, 0, persistent);
	}

	public SharedObject(Map<String, Object> data, String name, String path,
			boolean persistent, IPersistenceStore storage) {
		this.data = new ConcurrentHashMap<String, Object>();
		this.data.putAll(data);
		this.name = name;
		this.path = path;
		this.persistentSO = persistent;
		setStore(storage);

		ownerMessage = new SharedObjectMessage(null, name, 0, persistent);
	}

	/** {@inheritDoc} */
    public String getName() {
		return name;
	}

	/** {@inheritDoc} */
    public void setName(String name) {
		// Shared objects don't support setting of their names
	}

	/** {@inheritDoc} */
    public String getPath() {
		return path;
	}

	/** {@inheritDoc} */
    public void setPath(String path) {
		this.path = path;
	}

	/** {@inheritDoc} */
    public String getType() {
		return TYPE;
	}

	/** {@inheritDoc} */
    public long getLastModified() {
		return lastModified;
	}

	/**
     * Getter for property 'persistentObject'.
     *
     * @return Value for property 'persistentObject'.
     */
    public boolean isPersistentObject() {
		return persistentSO;
	}

	/** {@inheritDoc} */
    public boolean isPersistent() {
		return persistent;
	}

	/** {@inheritDoc} */
    public void setPersistent(boolean persistent) {
		this.persistent = persistent;
	}

	protected void sendUpdates() {
		if (!ownerMessage.getEvents().isEmpty()) {
			// Send update to "owner" of this update request
			SharedObjectMessage syncOwner = new SharedObjectMessage(null, name,
					version, isPersistentObject());
			syncOwner.addEvents(ownerMessage.getEvents());

			if (source != null) {
				// Only send updates when issued through RTMP request
				Channel channel = ((RTMPConnection) source)
						.getChannel((byte) 3);

				if (channel != null) {
					//ownerMessage.acquire();

					channel.write(syncOwner);
					log.debug("Owner: " + channel);
				} else {
					log.warn("No channel found for owner changes!?");
				}
			}
			ownerMessage.getEvents().clear();
		}
		
		if (!syncEvents.isEmpty()) {
			// Synchronize updates with all registered clients of this shared

			for (IEventListener listener : listeners) {

				if (listener == source) {
					// Don't re-send update to active client
					log.debug("Skipped " + source);
					continue;
				}

				if (!(listener instanceof RTMPConnection)) {
					log.warn("Can't send sync message to unknown connection "
							+ listener);
					continue;
				}

				// Create a new sync message for every client to avoid
				// concurrent access through multiple threads
				// TODO: perhaps we could cache the generated message
				SharedObjectMessage syncMessage = new SharedObjectMessage(null,
						name, version, isPersistentObject());
				syncMessage.addEvents(syncEvents);

				Channel c = ((RTMPConnection) listener).getChannel((byte) 3);
				log.debug("Send to " + c);
				c.write(syncMessage);
			}
			// Clear list of sync events
			syncEvents.clear();
		}
	}

	private void updateHashes() {
		hashes.clear();
		for (String name : data.keySet()) {
			Object value = data.get(name);
			hashes.put(name, value != null ? value.hashCode() : 0);
		}
	}

	protected void notifyModified() {
		if (updateCounter > 0) {
			// we're inside a beginUpdate...endUpdate block
			return;
		}

		if (modified) {
			// The client sent at least one update -> increase version of SO
			updateVersion();
			lastModified = System.currentTimeMillis();
		}

		if (modified && storage != null) {
			if (!storage.save(this)) {
				log.error("Could not store shared object.");
			}
		}

		sendUpdates();
		updateHashes();
	}

	public boolean hasAttribute(String name) {
		return data.containsKey(name);
	}

	/**
     * Getter for property 'attributeNames'.
     *
     * @return Value for property 'attributeNames'.
     */
    public Set<String> getAttributeNames() {
		return Collections.unmodifiableSet(data.keySet());
	}

	public Map<String, Object> getAttributes() {
		return Collections.unmodifiableMap(data);
	}

	public Object getAttribute(String name) {
		return data.get(name);
	}

	public synchronized boolean setAttribute(String name, Object value) {
		ownerMessage.addEvent(Type.CLIENT_UPDATE_ATTRIBUTE, name, null);
		Object old = data.get(name);
		Integer oldHash = (value != null ? value.hashCode() : 0);
		if (old == null || !old.equals(value)
				|| !oldHash.equals(hashes.get(name))) {
			modified = true;
			data.put(name, value);
			// only sync if the attribute changed
			syncEvents.add(new SharedObjectEvent(Type.CLIENT_UPDATE_DATA, name,
					value));
			notifyModified();
			return true;
		} else {
			notifyModified();
			return false;
		}
	}

	/**
     * Setter for property 'attributes'.
     *
     * @param values Value to set for property 'attributes'.
     */
    public synchronized void setAttributes(Map values) {
		if (values == null) {
			return;
		}

		beginUpdate();
		Iterator it = values.keySet().iterator();
		while (it.hasNext()) {
			String name = (String) it.next();
			setAttribute(name, values.get(name));
		}
		endUpdate();
	}

	/**
     * Setter for property 'attributes'.
     *
     * @param values Value to set for property 'attributes'.
     */
    public synchronized void setAttributes(IAttributeStore values) {
		if (values == null) {
			return;
		}

		beginUpdate();
		Iterator it = values.getAttributeNames().iterator();
		while (it.hasNext()) {
			String name = (String) it.next();
			setAttribute(name, values.getAttribute(name));
		}
		endUpdate();
	}

	public synchronized boolean removeAttribute(String name) {
		boolean result = data.containsKey(name);
		if (result) {
			data.remove(name);
		}
		// Send confirmation to client
		ownerMessage.addEvent(Type.CLIENT_DELETE_DATA, name, null);
		if (result) {
			modified = true;
			syncEvents.add(new SharedObjectEvent(Type.CLIENT_DELETE_DATA, name,
					null));
		}
		notifyModified();
		return result;
	}

	public synchronized void sendMessage(String handler, List arguments) {
		ownerMessage.addEvent(Type.CLIENT_SEND_MESSAGE, handler, arguments);
		syncEvents.add(new SharedObjectEvent(Type.CLIENT_SEND_MESSAGE, handler,
				arguments));
	}

	/**
     * Getter for property 'data'.
     *
     * @return Value for property 'data'.
     */
    public Map<String, Object> getData() {
		return Collections.unmodifiableMap(data);
	}

	/**
     * Getter for property 'version'.
     *
     * @return Value for property 'version'.
     */
    public int getVersion() {
		return version;
	}

	private void updateVersion() {
		version += 1;
	}

	public synchronized void removeAttributes() {
		// TODO: there must be a direct way to clear the SO on the client
		// side...
		Iterator keys = data.keySet().iterator();
		while (keys.hasNext()) {
			String key = (String) keys.next();
			ownerMessage.addEvent(Type.CLIENT_DELETE_DATA, key, null);
			syncEvents.add(new SharedObjectEvent(Type.CLIENT_DELETE_DATA, key,
					null));
		}

		data.clear();
		modified = true;
		notifyModified();
	}

	public synchronized void register(IEventListener listener) {
		listeners.add(listener);

		// prepare response for new client
		ownerMessage.addEvent(Type.CLIENT_INITIAL_DATA, null, null);
		if (!isPersistentObject()) {
			ownerMessage.addEvent(Type.CLIENT_CLEAR_DATA, null, null);
		}
		if (!data.isEmpty()) {
			ownerMessage.addEvent(new SharedObjectEvent(
					Type.CLIENT_UPDATE_DATA, null, getData()));
		}

		// we call notifyModified here to send response if we're not in a
		// beginUpdate block
		notifyModified();
	}

	public synchronized void unregister(IEventListener listener) {
		listeners.remove(listener);
		if (!isPersistentObject() && listeners.isEmpty()) {
			log.info("Deleting shared object " + name
					+ " because all clients disconnected.");
			data.clear();
			if (storage != null) {
				if (!storage.remove(this)) {
					log.error("Could not remove shared object.");
				}
			}
		}
	}

	/**
     * Getter for property 'listeners'.
     *
     * @return Value for property 'listeners'.
     */
    public HashSet getListeners() {
		return listeners;
	}

	public void beginUpdate() {
		beginUpdate(source);
	}

	public synchronized void beginUpdate(IEventListener listener) {
		source = listener;
		updateCounter += 1;
	}

	public synchronized void endUpdate() {
		updateCounter -= 1;

		if (updateCounter == 0) {
			notifyModified();
			source = null;
		}
	}

	/** {@inheritDoc} */
    public void serialize(Output output) throws IOException {
		Serializer ser = new Serializer();
		ser.serialize(output, getName());
		ser.serialize(output, data);
	}

	/** {@inheritDoc} */
    public void deserialize(Input input) throws IOException {
		Deserializer deserializer = new Deserializer();
		name = (String) deserializer.deserialize(input);
		persistentSO = persistent = true;
		data.clear();
		data.putAll((Map<String, Object>) deserializer.deserialize(input));
		updateHashes();
		ownerMessage.setName(name);
		ownerMessage.setIsPersistent(true);
	}

	/** {@inheritDoc} */
    public void setStore(IPersistenceStore store) {
		this.storage = store;
	}

	/** {@inheritDoc} */
    public IPersistenceStore getStore() {
		return storage;
	}

	/**
	 * Deletes all the attributes and sends a clear event to all listeners. The
	 * persistent data object is also removed from a persistent shared object.
	 * 
	 * @return true if successful; false otherwise
	 */
	public synchronized boolean clear() {
		data.clear();
		// Send confirmation to client
		ownerMessage.addEvent(Type.CLIENT_CLEAR_DATA, name, null);
		return data.isEmpty();
	}

	/**
	 * Detaches a reference from this shared object, this will destroy the
	 * reference immediately. This is useful when you don't want to proxy a
	 * shared object any longer.
	 */
	public synchronized void close() {
		// clear collections
		data.clear();
		listeners.clear();
		hashes.clear();
		syncEvents.clear();
		// dereference objects
		data = null;
		listeners = null;
		hashes = null;
		ownerMessage = null;
		source = null;
		syncEvents = null;
		storage = null;
	}

}
