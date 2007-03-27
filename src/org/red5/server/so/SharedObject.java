package org.red5.server.so;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2007 by respective authors (see below). All rights reserved.
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

/**
 * Represents shared object on server-side. Shared Objects in Flash are like cookies that are stored
 * on client side. In Red5 and Flash Media Server there's one more special type of SOs : remote Shared Objects.
 *
 * These are shared by multiple clients and synchronized between them automatically on each data change. This is done
 * asynchronously, used as events handling and is widely used in multiplayer Flash online games.
 *
 * Shared object can be persistent or transient. The difference is that first are saved to the disk and can be
 * accessed later on next connection, transient objects are not saved and get lost each time they last client
 * disconnects from it.
 *
 * Shared Objects has name identifiers and path on server's HD (if persistent). On deeper level server-side
 * Shared Object in this implementation actually uses IPersistenceStore to delegate all (de)serialization work.
 *
 * SOs store data as simple map, that is, "name-value" pairs. Each value in turn can be complex object or map.
 */
public class SharedObject implements IPersistable, Constants {
    /**
     * Logger
     */
	protected static Log log = LogFactory.getLog(SharedObject.class.getName());

    /**
     * Shared Object name (identifier)
     */
    protected String name = "";

    /**
     * SO path
     */
    protected String path = "";

    /**
     * true if the SharedObject was stored by the persistence framework (NOT in database,
     * just plain serialization to the disk) and can be used later on reconnection
     */

	protected boolean persistent;

    /**
     * true if the client / server created the SO to be persistent
     */
	protected boolean persistentSO;

    /**
     * Object that is delegated with all storage work for persistent SOs
     */
    protected IPersistenceStore storage;

    /**
     * Version. Used on synchronization purposes.
     */
    protected int version = 1;

    /**
     * SO data
     */
    protected Map<String, Object> data;

    /**
     * SO hashes
     */
    protected Map<String, Integer> hashes = new HashMap<String, Integer>();

    /**
     * Number of pending update operations
     */
    protected int updateCounter;

    /**
     * Has changes? flag
     */
    protected boolean modified;

    /**
     * Last modified timestamp
     */
    protected long lastModified = -1;

    /**
     * Owner event
     */
    protected SharedObjectMessage ownerMessage;

    /**
     * Synchronization events
     */
    protected LinkedList<ISharedObjectEvent> syncEvents = new LinkedList<ISharedObjectEvent>();

    /**
     * Listeners
     */
    protected HashSet<IEventListener> listeners = new HashSet<IEventListener>();

    /**
     * Event listener, actually RTMP connection
     */
    protected IEventListener source;
    
    /**
     * Number of times the SO has been acquired
     */
    protected int acquireCount;

	/** Constructs a new SharedObject. */
    public SharedObject() {
		// This is used by the persistence framework
        data = null;
        data = new ConcurrentHashMap<String, Object>();

		ownerMessage = new SharedObjectMessage(null, null, -1, false);
        persistentSO = false;
    }

    /**
     * Constructs new SO from Input object
     * @param input              Input source
     * @throws IOException       I/O exception
     *
     * @see org.red5.io.object.Input
     */
    public SharedObject(Input input) throws IOException {
		this();
		deserialize(input);
	}

    /**
     * Creates new SO from given data map, name, path and persistence option
     *
     * @param data               Data
     * @param name               SO name
     * @param path               SO path
     * @param persistent         SO persistence
     */
    public SharedObject(Map<String, Object> data, String name, String path,
			boolean persistent) {
        this.data = null;
        this.data = new ConcurrentHashMap<String, Object>();
		this.data.putAll(data);
		this.name = name;
		this.path = path;
        persistentSO = false;
        this.persistentSO = persistent;

		ownerMessage = new SharedObjectMessage(null, name, 0, persistent);
	}

    /**
     * Creates new SO from given data map, name, path, storage object and persistence option
     * @param data               Data
     * @param name               SO name
     * @param path               SO path
     * @param persistent         SO persistence
     * @param storage            Persistence storage
     */
    public SharedObject(Map<String, Object> data, String name, String path,
			boolean persistent, IPersistenceStore storage) {
        this.data = null;
        this.data = new ConcurrentHashMap<String, Object>();
		this.data.putAll(data);
		this.name = name;
		this.path = path;
        persistentSO = false;
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
     * Getter for persistent object
     *
     * @return  Persistent object
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

    /**
     * Send update notification over data channel of RTMP connection
     */
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

    /**
     * Update hashes
     */
    private void updateHashes() {
		hashes.clear();
		for (String name : data.keySet()) {
			Object value = data.get(name);
			hashes.put(name, value != null ? value.hashCode() : 0);
		}
	}

    /**
     * Send notification about modification of SO
     */
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

    /**
     * Return an error message to the client.
     * 
     * @param message
     */
    protected void returnError(String message) {
		ownerMessage.addEvent(Type.CLIENT_STATUS, "error", message);
    }
    
    /**
     * Return an attribute value to the owner.
     * 
     * @param name
     */
    protected void returnAttributeValue(String name) {
		ownerMessage.addEvent(Type.CLIENT_UPDATE_DATA, name, getAttribute(name));
    }
    
    /**
     * Check whether this SO has given attribute
     * @param name          Attribute name
     * @return              <code>true</code> if this SO has attribute with given name, <code>false</code> otherwise
     */
    public boolean hasAttribute(String name) {
		return data.containsKey(name);
	}

	/**
     * Return attribute names as set.
     *
     * @return  Set of attribute names
     */
    public Set<String> getAttributeNames() {
		return Collections.unmodifiableSet(data.keySet());
	}

    /**
     * Return map of attributes of this SO
     * @return   Map of attributes
     */
    public Map<String, Object> getAttributes() {
		return Collections.unmodifiableMap(data);
	}

    /**
     * Return attribute by name
     * @param name         Attribute name
     * @return             Attribute value
     */
    public Object getAttribute(String name) {
		return data.get(name);
	}

    /**
     * Set value of attribute with given name
     * @param name         Attribute name
     * @param value        Attribute value
     * @return             <code>true</code> if there's such attribute and value was set, <code>false</code> otherwise
     */
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
     * Set attributes as map.
     *
     * @param values  Attributes.
     */
    public synchronized void setAttributes(Map<String, Object> values) {
		if (values == null) {
			return;
		}

		beginUpdate();
        for (String name : values.keySet()) {
            setAttribute(name, values.get(name));
        }
        endUpdate();
	}

	/**
     * Set attributes as attributes store.
     *
     * @param values  Attributes.
     */
    public synchronized void setAttributes(IAttributeStore values) {
		if (values == null) {
			return;
		}

		beginUpdate();
        for (String name : values.getAttributeNames()) {
            setAttribute(name, values.getAttribute(name));
        }
        endUpdate();
	}

    /**
     * Removes attribute with given name
     * @param name    Attribute
     * @return        <code>true</code> if there's such an attribute and it was removed, <code>false</code> otherwise
     */
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

    /**
     * Broadcast event to event handler
     * @param handler         Event handler
     * @param arguments       Arguments
     */
    public synchronized void sendMessage(String handler, List arguments) {
        // Forward
        ownerMessage.addEvent(Type.CLIENT_SEND_MESSAGE, handler, arguments);
		syncEvents.add(new SharedObjectEvent(Type.CLIENT_SEND_MESSAGE, handler,
				arguments));
	}

	/**
     * Getter for data.
     *
     * @return  SO data as unmodifiable map
     */
    public Map<String, Object> getData() {
		return Collections.unmodifiableMap(data);
	}

	/**
     * Getter for version.
     *
     * @return  SO version.
     */
    public int getVersion() {
		return version;
	}

    /**
     * Increases version by one
     */
	private void updateVersion() {
		version += 1;
	}

    /**
     * Remove all attributes (clear Shared Object)
     */
    public synchronized void removeAttributes() {
		// TODO: there must be a direct way to clear the SO on the client side...
        for (String key : data.keySet()) {
            ownerMessage.addEvent(Type.CLIENT_DELETE_DATA, key, null);
            syncEvents.add(new SharedObjectEvent(Type.CLIENT_DELETE_DATA, key,
                    null));
        }
        // Clear data
		data.clear();
        // Mark as modified
        modified = true;
        // Broadcast 'modified' event
        notifyModified();
	}

    /**
     * Register event listener
     * @param listener        Event listener
     */
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

    /**
     * Check if shared object must be released.
     */
    protected void checkRelease() {
		if (!isPersistentObject() && listeners.isEmpty() && !isAcquired()) {
			log.info("Deleting shared object " + name
					+ " because all clients disconnected and it is no longer acquired.");
			if (storage != null) {
				if (!storage.remove(this)) {
					log.error("Could not remove shared object.");
				}
			}
			close();
		}
    }
    
    /**
     * Unregister event listener
     * @param listener        Event listener
     */
    public synchronized void unregister(IEventListener listener) {
		listeners.remove(listener);
		checkRelease();
	}

	/**
     * Get event listeners.
     *
     * @return Value for property 'listeners'.
     */
    public HashSet<IEventListener> getListeners() {
		return listeners;
	}

    /**
     * Begin update of this Shared Object.
     * Increases number of pending update operations
     */
    public void beginUpdate() {
		beginUpdate(source);
	}

    /**
     * Begin update of this Shared Object and setting listener
     * @param listener      Update with listener
     */
    public synchronized void beginUpdate(IEventListener listener) {
		source = listener;
        // Increase number of pending updates
        updateCounter += 1;
	}

    /**
     * End update of this Shared Object. Decreases number of pending update operations and
     * broadcasts modified event if it is equal to zero (i.e. no more pending update operations).
     */
    public synchronized void endUpdate() {
        // Decrease number of pending updates
        updateCounter -= 1;

        // If
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
	 * @return <code>true</code> on success, <code>false</code> otherwise
	 */
	public synchronized boolean clear() {
		data.clear();
		// Send confirmation to client
		ownerMessage.addEvent(Type.CLIENT_CLEAR_DATA, name, null);
        // Is it clear now?
        return data.isEmpty();
	}

	/**
	 * Detaches a reference from this shared object, reset it's state, this will destroy the
	 * reference immediately. This is useful when you don't want to proxy a
	 * shared object any longer.
	 */
	public synchronized void close() {
		// clear collections
		data.clear();
		listeners.clear();
		hashes.clear();
		syncEvents.clear();
		ownerMessage.getEvents().clear();
	}

	/**
	 * Prevent shared object from being released. Each call to <code>acquire</code>
	 * must be paired with a call to <code>release</code> so the SO isn't held
	 * forever. This is only valid for non-persistent SOs.
	 */
	public synchronized void acquire() {
		acquireCount++;
	}
	
	/**
	 * Check if shared object currently is acquired.
	 * 
	 * @return <code>true</code> if the SO is acquired, otherwise <code>false</code>
	 */
	public synchronized boolean isAcquired() {
		return acquireCount > 0;
	}
	
	/**
	 * Release previously acquired shared object. If the SO is non-persistent,
	 * no more clients are connected the SO isn't acquired any more, the data
	 * is released. 
	 */
	public synchronized void release() {
		if (acquireCount == 0)
			throw new RuntimeException("The shared object was not acquired before.");
		
		acquireCount--;
		if (acquireCount == 0)
			checkRelease();
	}
}
