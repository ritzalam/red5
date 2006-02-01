package org.red5.server.context;

import java.util.HashMap;
import java.util.HashSet;

public class PersistentSharedObject {

	protected String name;
	protected int version = 0;
	protected HashMap data = new HashMap();
	protected HashMap clients = new HashMap();
	
	public PersistentSharedObject(String name) {
		this.name = name;
	}
	
	public void updateAttribute(String name, Object value) {
		this.data.put(name, value);
	}
	
	public void deleteAttribute(String name) {
		this.data.remove(name);
	}
	
	public HashMap getData() {
		return this.data;
	}
	
	public int getVersion() {
		return this.version;
	}
	
	public void updateVersion() {
		this.version += 1;
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
