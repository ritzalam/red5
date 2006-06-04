package org.red5.server.so;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.io.object.Deserializer;
import org.red5.io.object.Input;
import org.red5.io.object.Output;
import org.red5.io.object.Serializer;
import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.event.IEventListener;
import org.red5.server.net.rtmp.Channel;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.SharedObjectEvent;
import org.red5.server.api.persistence.IPersistable;
import org.red5.server.api.persistence.IPersistenceStore;

public class SharedObject implements IPersistable, Constants {

	protected static Log log = LogFactory.getLog(SharedObject.class.getName());

	public final static String PERSISTENCE_TYPE = "SharedObject";

	protected String name = "";
	protected String path = "";
	// true if the SharedObject was stored by the persistence framework 
	protected boolean persistent = false;
	// true if the client / server created the SO to be persistent
	protected boolean persistentSO = false;
	protected IPersistenceStore storage = null;

	protected int version = 0;
	protected Map<String, Object> data = null;
	protected int updateCounter = 0;
	protected boolean modified = false;
	protected long lastModified = -1;
	
	private org.red5.server.net.rtmp.message.SharedObject ownerMessage;
	private LinkedList<SharedObjectEvent> syncEvents = new LinkedList<SharedObjectEvent>();
	
	protected HashSet<IEventListener> listeners = new HashSet<IEventListener>();
	private IEventListener source = null;

	public SharedObject() {
		// This is used by the persistence framework
		data = new HashMap<String, Object>();
		
		ownerMessage = new org.red5.server.net.rtmp.message.SharedObject();
		ownerMessage.setTimestamp(0);
	}

	public SharedObject(Input input) throws IOException {
		this();
		deserialize(input);
	}

	public SharedObject(Map<String, Object> data, String name, String path, boolean persistent) {
		this.data = data;
		this.name = name;
		this.path = path;
		this.persistentSO = persistent;
		
		ownerMessage = new org.red5.server.net.rtmp.message.SharedObject();
		ownerMessage.setName(name);
		ownerMessage.setTimestamp(0);
		ownerMessage.setType(persistent ? 2 : 0);
	}

	public SharedObject(Map<String, Object> data, String name, String path, boolean persistent,
			IPersistenceStore storage) {
		this.data = data;
		this.name = name;
		this.path = path;
		this.persistentSO = persistent;
		setStore(storage);

		ownerMessage = new org.red5.server.net.rtmp.message.SharedObject();
		ownerMessage.setName(name);
		ownerMessage.setTimestamp(0);
		ownerMessage.setType(persistent ? 2 : 0);
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		// Shared objects don't support setting of their names
	}
	
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getType() {
		return PERSISTENCE_TYPE;
	}

	public long getLastModified() {
		return lastModified;
	}

	public boolean isPersistentObject() {
		return persistentSO;
	}

	public boolean isPersistent() {
		return persistent;
	}

	public void setPersistent(boolean persistent) {
		this.persistent = persistent;
	}

	private void sendUpdates() {
		if (!ownerMessage.getEvents().isEmpty()) {
			// Send update to "owner" of this update request
			org.red5.server.net.rtmp.message.SharedObject syncOwner  = new org.red5.server.net.rtmp.message.SharedObject();
			syncOwner.setName(name);
			syncOwner.setTimestamp(0);
			syncOwner.setType(isPersistentObject() ? 2 : 0);
			syncOwner.setSoId(version);
			syncOwner.setSealed(false);
			syncOwner.addEvents(ownerMessage.getEvents());

			if (source != null) {
				// Only send updates when issued through RTMP request
				Channel channel = ((RTMPConnection) source).getChannel((byte) 3);
				
				if (channel != null) {
					//ownerMessage.acquire();
	
					channel.write(syncOwner);
					log.debug("Owner: " + channel);
				} else
					log.warn("No channel found for owner changes!?");
			}
			ownerMessage.getEvents().clear();
		}

		if (!syncEvents.isEmpty()) {
			// Synchronize updates with all registered clients of this shared
			
			for(IEventListener listener : listeners) {
				
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
				org.red5.server.net.rtmp.message.SharedObject syncMessage = new org.red5.server.net.rtmp.message.SharedObject();
				syncMessage.setName(name);
				syncMessage.setSoId(version);
				syncMessage.setTimestamp(0);
				syncMessage.setType(isPersistentObject() ? 2 : 0);
				syncMessage.addEvents(syncEvents);
				
				Channel c = ((RTMPConnection) listener).getChannel((byte) 3);
				log.debug("Send to " + c);
				c.write(syncMessage);
			}
			// Clear list of sync events
			syncEvents.clear();
		}
	}

	private void notifyModified() {
		if (updateCounter > 0)
			// we're inside a beginUpdate...endUpdate block
			return;

		if (modified) {
			// The client sent at least one update -> increase version of SO
			updateVersion();
			lastModified = System.currentTimeMillis();
		}

		if (modified && storage != null) {
			if (!storage.save(this))
				log.error("Could not store shared object.");
		}

		sendUpdates();
	}

	public boolean hasAttribute(String name) {
		return data.containsKey(name);
	}

	public Set getAttributeNames() {
		return data.keySet();
	}

	public Object getAttribute(String name) {
		return data.get(name);
	}

	public boolean setAttribute(String name, Object value) {
		ownerMessage.addEvent(new SharedObjectEvent(
				SO_CLIENT_UPDATE_ATTRIBUTE, name, null));
		Object old = data.get(name);
		if (old == null || !old.equals(value)) {
			modified = true;
			data.put(name, value);
			// only sync if the attribute changed
			syncEvents.add(new SharedObjectEvent(
					SO_CLIENT_UPDATE_DATA, name, value));
			notifyModified();
			return true;
		} else {
			notifyModified();
			return false;
		}
	}

	public void setAttributes(Map values) {
		beginUpdate();
		Iterator it = values.keySet().iterator();
		while (it.hasNext()) {
			String name = (String) it.next();
			setAttribute(name, values.get(name));
		}
		endUpdate();
	}

	public void setAttributes(org.red5.server.api.IAttributeStore values) {
		beginUpdate();
		Iterator it = values.getAttributeNames().iterator();
		while (it.hasNext()) {
			String name = (String) it.next();
			setAttribute(name, values.getAttribute(name));
		}
		endUpdate();
	}

	public boolean removeAttribute(String name) {
		boolean result = data.containsKey(name);
		if (result)
			data.remove(name);
		// Send confirmation to client
		ownerMessage.addEvent(new SharedObjectEvent(SO_CLIENT_DELETE_DATA,
				name, null));
		if (result) {
			modified = true;
			syncEvents.add(new SharedObjectEvent(
					SO_CLIENT_DELETE_DATA, name, null));
		}
		notifyModified();
		return result;
	}

	public void sendMessage(String handler, List arguments) {
		ownerMessage.addEvent(new SharedObjectEvent(
				SO_CLIENT_SEND_MESSAGE, handler, arguments));
		syncEvents.add(new SharedObjectEvent(
				SO_CLIENT_SEND_MESSAGE, handler, arguments));
	}

	public Map<String, Object> getData() {
		Map<String, Object> result = new HashMap<String, Object>();
		Iterator<String> it = data.keySet().iterator();
		while (it.hasNext()) {
			String name = it.next();
			Object value = data.get(name);
			result.put(name, value);
		}
		return result;
	}

	public int getVersion() {
		return version;
	}

	private void updateVersion() {
		version += 1;
	}

	public void removeAttributes() {
		// TODO: there must be a direct way to clear the SO on the client
		// side...
		Iterator keys = data.keySet().iterator();
		while (keys.hasNext()) {
			String key = (String) keys.next();
			ownerMessage.addEvent(new SharedObjectEvent(
					SO_CLIENT_DELETE_DATA, key, null));
			syncEvents.add(new SharedObjectEvent(
					SO_CLIENT_DELETE_DATA, key, null));
		}

		data.clear();
		modified = true;
		notifyModified();
	}

	public void register(IEventListener listener) {
		listeners.add(listener);

		// prepare response for new client
		ownerMessage.addEvent(new SharedObjectEvent(
				SO_CLIENT_INITIAL_DATA, null, null));
		if (!data.isEmpty())
			ownerMessage.addEvent(new SharedObjectEvent(
					SO_CLIENT_UPDATE_DATA, null, getData()));

		// we call notifyModified here to send response if we're not in a
		// beginUpdate block
		notifyModified();
	}

	public void unregister(IEventListener listener) {
		listeners.remove(listener);
		if (!isPersistentObject() && listeners.isEmpty()) {
			log.info("Deleting shared object " + name
					+ " because all clients disconnected.");
			data.clear();
			if (storage != null) {
				if (!storage.remove(this))
					log.error("Could not remove shared object.");
			}
		}
	}

	public HashSet getListeners() {
		return listeners;
	}

	public void beginUpdate() {
		beginUpdate(null);
	}

	public void beginUpdate(IEventListener listener) {
		source = listener;
		updateCounter += 1;
	}

	public void endUpdate() {
		updateCounter -= 1;

		if (updateCounter == 0)
			notifyModified();
	}

	public void serialize(Output output) throws IOException {
		Serializer ser = new Serializer();
		ser.serialize(output, getName());
		ser.serialize(output, data);
	}

	public void deserialize(Input input) throws IOException {
		Deserializer deserializer = new Deserializer();
		name = (String) deserializer.deserialize(input);
		persistentSO = persistent = true;
		data.clear();
		data.putAll((Map<String, Object>) deserializer.deserialize(input));
		ownerMessage.setName(name);
		ownerMessage.setType(2);
	}

	public void setStore(IPersistenceStore store) {
		this.storage = store;
	}
	
	public IPersistenceStore getStore() {
		return storage;
	}
}
