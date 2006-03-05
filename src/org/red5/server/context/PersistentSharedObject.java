package org.red5.server.context;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.api.SharedObject;
import org.red5.server.net.rtmp.Channel;
import org.red5.server.net.rtmp.BaseConnection;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.SharedObjectEvent;
import org.red5.server.SharedObjectPersistence;

public class PersistentSharedObject implements SharedObject, Constants {

	protected static Log log =
        LogFactory.getLog(PersistentSharedObject.class.getName());

	protected String name;
	protected SharedObjectPersistence persistence = null;
	protected int version = 0;
	protected boolean persistent = true;
	protected HashMap data = new HashMap();
	protected HashMap clients = new HashMap();
	protected int updateCounter = 0;
	protected boolean modified = false;
	
	private org.red5.server.net.rtmp.message.SharedObject ownerMessage;
	private org.red5.server.net.rtmp.message.SharedObject syncMessage;
	
	public PersistentSharedObject(String name, boolean persistent, SharedObjectPersistence persistence) {
		this.name = name;
		this.persistent = persistent;
		this.persistence = persistence;
		
		this.ownerMessage = new org.red5.server.net.rtmp.message.SharedObject();
		this.ownerMessage.setName(name);
		this.ownerMessage.setTimestamp(0);
		this.ownerMessage.setType(persistent ? 2 : 0);
		
		this.syncMessage = new org.red5.server.net.rtmp.message.SharedObject();
		this.syncMessage.setName(name);
		this.syncMessage.setTimestamp(0);
		this.syncMessage.setType(persistent ? 2 : 0);
	}
	
	public String getName() {
		return this.name;
	}
	
	public boolean isPersistent() {
		return this.persistent;
	}
	
	private void sendUpdates() {
		BaseConnection conn = (BaseConnection) Scope.getClient();
		if (!this.ownerMessage.getEvents().isEmpty()) {
			// Send update to "owner" of this update request
			this.ownerMessage.setSoId(this.version);
			this.ownerMessage.setSealed(false);
			Channel channel = Scope.getChannel();
			if (channel != null) {
				channel.write(this.ownerMessage);
				log.debug("Owner: " + channel);
			} else
				log.warn("No channel found for owner changes!?");
			this.ownerMessage.getEvents().clear();
		}
		
		if (!this.syncMessage.getEvents().isEmpty()) {
			// Synchronize updates with all registered clients of this shared object
			this.syncMessage.setSoId(this.version);
			this.syncMessage.setSealed(false);
			// Acquire the packet, this will stop the data inside being released
			this.syncMessage.acquire();
			HashMap all_clients = this.clients;
			Iterator clients = all_clients.keySet().iterator();
			while (clients.hasNext()) {
				BaseConnection connection = (BaseConnection) clients.next();
				if (connection == conn) {
					// Don't re-send update to active client
					log.debug("Skipped " + connection);
					continue;
				}
				
				Iterator channels = ((HashSet) all_clients.get(connection)).iterator();
				while (channels.hasNext()) {
					Channel c = connection.getChannel(((Integer) channels.next()).byteValue());
					log.debug("Send to " + c);
					c.write(this.syncMessage);
					this.syncMessage.setSealed(false);
				}
			}
			// After sending the packet down all the channels we can release the packet, 
			// which in turn will allow the data buffer to be released
			this.syncMessage.release();
			this.syncMessage.getEvents().clear();
		}
	}
	
	private void notifyModified() {
		if (this.updateCounter > 0)
			// we're inside a beginUpdate...endUpdate block
			return;
		
		if (this.modified)
			// The client sent at least one update -> increase version of SO
			this.updateVersion();
		
		if (this.modified && this.persistence != null)
			this.persistence.storeSharedObject(this);
		
		this.sendUpdates();
	}
	
	public Object getAttribute(String name) {
		return this.data.get(name);
	}
	
	public boolean updateAttribute(String name, Object value) {
		Object old = this.data.get(name);
		// Send confirmation to client
		this.ownerMessage.addEvent(new SharedObjectEvent(SO_CLIENT_UPDATE_ATTRIBUTE, name, null));
		if ((old == null) || (!old.equals(value))) {
			this.data.put(name, value);
			this.modified = true;
			// only sync if the attribute changed 
			this.syncMessage.addEvent(new SharedObjectEvent(SO_CLIENT_UPDATE_DATA, name, value));
			this.notifyModified();
			return true;
		} else {
			this.notifyModified();
			return false;
		}
	}
	
	public boolean deleteAttribute(String name) {
		boolean result = this.data.containsKey(name);
		this.data.remove(name);
		// Send confirmation to client
		this.ownerMessage.addEvent(new SharedObjectEvent(SO_CLIENT_DELETE_DATA, name, null));
		if (result) {
			this.modified = true;
			this.syncMessage.addEvent(new SharedObjectEvent(SO_CLIENT_DELETE_DATA, name, null));
		}
		this.notifyModified();
		return result;
	}
	
	public void sendMessage(String handler, List arguments) {
		this.ownerMessage.addEvent(new SharedObjectEvent(SO_CLIENT_SEND_MESSAGE, handler, arguments));
		this.syncMessage.addEvent(new SharedObjectEvent(SO_CLIENT_SEND_MESSAGE, handler, arguments));
	}
	
	public void setData(Map data) {
		this.data.clear();
		this.data.putAll(data);
		this.modified = false;
	}
	
	public HashMap getData() {
		return this.data;
	}
	
	public int getVersion() {
		return this.version;
	}
	
	private void updateVersion() {
		this.version += 1;
	}
	
	public void clear() {
		// TODO: there must be a direct way to clear the SO on the client side...
		Iterator keys = this.data.keySet().iterator();
		while (keys.hasNext()) {
			String key = (String) keys.next();
			this.ownerMessage.addEvent(new SharedObjectEvent(SO_CLIENT_DELETE_DATA, key, null));
			this.syncMessage.addEvent(new SharedObjectEvent(SO_CLIENT_DELETE_DATA, key, null));
		}
		
		this.data.clear();
		this.modified = true;
		this.notifyModified();
	}
	
	public void registerClient(Client client, int channel) {
		if (!this.clients.containsKey(client))
			this.clients.put(client, new HashSet());
		
		HashSet channels = (HashSet) this.clients.get(client);
		channels.add(new Integer(channel));
		
		// prepare response for new client
		this.ownerMessage.addEvent(new SharedObjectEvent(SO_CLIENT_INITIAL_DATA, null, null));
		if (!this.data.isEmpty())
			this.ownerMessage.addEvent(new SharedObjectEvent(SO_CLIENT_UPDATE_DATA, null, this.data));
		
		// we call notifyModified here to send response if we're not in a beginUpdate block
		this.notifyModified();
	}
	
	public void unregisterClient(Client client) {
		this.clients.remove(client);
		if (!this.persistent && this.clients.isEmpty()) {
			log.info("Deleting shared object " + this.name + " because all clients disconnected.");
			this.data.clear();
			this.persistence.deleteSharedObject(this.name);
		}
	}
	
	public void unregisterClient(Client client, int channel) {
		if (!this.clients.containsKey(client))
			// No channel registered for this client
			return;
		
		HashSet channels = (HashSet) this.clients.get(client);
		channels.remove(new Integer(channel));
		if (channels.isEmpty()) {
			// Delete shared object in case of non-persistent SOs
			this.unregisterClient(client);
		}
	}
	
	public HashMap getClients() {
		return this.clients;
	}
	
	public void beginUpdate() {
		this.updateCounter += 1;
	}
	
	public void endUpdate() {
		this.updateCounter -= 1;
		
		if (this.updateCounter == 0)
			this.notifyModified();
	}
}
