package org.red5.server;

import java.util.HashMap;

import org.red5.server.api.IClient;
import org.red5.server.api.IClientRegistry;

public class ClientRegistry implements IClientRegistry {

	private HashMap<String,IClient> clients = new HashMap<String,IClient>();
	private int nextId = 0;
	
	public synchronized String nextId(){
		return "" + nextId++;
	}
	
	public boolean hasClient(String id) {
		return clients.containsKey(id);
	}

	public IClient lookupClient(String id) {
		return clients.get(id);
	}

	public IClient newClient(Object[] params) {
		IClient client = new Client(nextId(), this);
		addClient(client);
		return client;
	}

	protected void addClient(IClient client) {
		clients.put(client.getId(), client);
	}
	
	protected void removeClient(IClient client) {
		clients.remove(client.getId());
	}
	
}