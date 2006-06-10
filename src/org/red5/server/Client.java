package org.red5.server;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.api.IConnection;
import org.red5.server.api.IClient;
import org.red5.server.api.IScope;

public class Client extends AttributeStore  
	implements IClient {

	protected static Log log =
        LogFactory.getLog(Client.class.getName());
	
	protected String id;
	protected long creationTime;
	protected ClientRegistry registry;
	protected HashMap<IConnection,IScope> connToScope = new HashMap<IConnection,IScope>();
	
	public Client(String id, ClientRegistry registry){
		this.id = id;
		this.registry = registry;
		this.creationTime = System.currentTimeMillis();
	}

	public String getId() {
		return id;
	}

	public long getCreationTime() {
		return creationTime;
	}
	
	public boolean equals(Object obj) {
		if(!(obj instanceof Client)) return false;
		final Client client = (Client) obj;
		return client.getId() == getId();
 	}

	public String toString() {
		return "Client: "+id;
	}
	
	public Set<IConnection> getConnections() {
		return connToScope.keySet();
	}
	
	public Set<IConnection> getConnections(IScope scope) {
		if (scope == null)
			return getConnections();
		
		Set<IConnection> result = new HashSet<IConnection>();
		for (Entry<IConnection, IScope> entry: connToScope.entrySet()) {
			if (scope.equals(entry.getValue()))
				result.add(entry.getKey());
		}
		return result;
	}

	public Collection<IScope> getScopes() {
		return connToScope.values();
	}

	public void disconnect() {
		log.debug("Disconnect, closing "+getConnections().size()+" connections");
		Iterator<IConnection> conns = getConnections().iterator();
		while(conns.hasNext()){
			conns.next().close();
		}
	}
		
	void register(IConnection conn){
		connToScope.put(conn, conn.getScope());
	}
	
	void unregister(IConnection conn){
		connToScope.remove(conn);
		if (connToScope.isEmpty()) {
			// This client is not connected to any scopes, remove from registry.
			registry.removeClient(this);
		}
	}

}