package org.red5.server;

import java.util.HashMap;

import org.red5.server.api.IClient;
import org.red5.server.api.IClientRegistry;

public class ClientRegistry implements IClientRegistry {

	private HashMap<String,IClient> clients = new HashMap<String,IClient>();
	private int nextId = 0;
	
	public String nextId(){
		return "" + nextId++;
	}
	
	public boolean hasClient(String id) {
		return false;
	}

	public IClient lookupClient(String id) {
		return null;
	}

	public IClient newClient() {
		IClient client = new Client(nextId(), this);
		clients.put(client.getId(), client);
		return client;
	}

	protected void removeClient(IClient client) {
		clients.remove(client.getId());
	}
	
}