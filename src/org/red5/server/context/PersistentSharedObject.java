package org.red5.server.context;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class PersistentSharedObject {

	protected String name;
	protected SharedObjectPersistence persistence = null;
	protected int version = 0;
	protected HashMap data = new HashMap();
	protected HashMap clients = new HashMap();
	
	public PersistentSharedObject(String name, SharedObjectPersistence persistence) {
		this.name = name;
		this.persistence = persistence;
	}
	
	public String getName() {
		return this.name;
	}
	
	private void notifyModified() {
		if (this.persistence != null)
			this.persistence.storeSharedObject(this);
	}
	
	public void updateAttribute(String name, Object value) {
		this.data.put(name, value);
		this.notifyModified();
	}
	
	public void deleteAttribute(String name) {
		this.data.remove(name);
		this.notifyModified();
	}
	
	public void setData(Map data) {
		this.data.clear();
		this.data.putAll(data);
	}
	
	public HashMap getData() {
		return this.data;
	}
	
	public int getVersion() {
		return this.version;
	}
	
	public void updateVersion() {
		this.version += 1;
		this.notifyModified();
	}
	
	public void registerClient(Object client, int channel) {
		if (!this.clients.containsKey(client))
			this.clients.put(client, new HashSet());
		
		HashSet channels = (HashSet) this.clients.get(client);
		channels.add(new Integer(channel));
	}
	
	public void unregisterClient(Object client) {
		this.clients.remove(client);
	}
	
	public void unregisterClient(Object client, int channel) {
		if (!this.clients.containsKey(client))
			// No channel registered for this client
			return;
		
		HashSet channels = (HashSet) this.clients.get(client);
		channels.remove(new Integer(channel));
	}
	
	public HashMap getClients() {
		return this.clients;
	}
}
