package org.red5.server.api.impl;

import java.util.HashMap;

import org.red5.server.api.Connection;
import org.red5.server.api.Scope;

public class Client extends AttributeStore  
	implements  org.red5.server.api.Client {

	protected String id;
	protected String host;
	protected long creationTime;
	protected HashMap scopeToConnMap = new HashMap();
	
	public Client(String id, String host){
		this.id = id;
		this.host = host;
		this.creationTime = System.currentTimeMillis();
	}

	public String getId() {
		return id;
	}
	
	public String getHost() {
		return host;
	}

	public long getCreationTime() {
		return creationTime;
	}

	public Connection lookupConnection(Scope scope){
		return (Connection) scopeToConnMap.get(scope);
	}
	
	/* at the moment only a single conn per scope allowed
	public Connection getConnection(Scope scope) {
		if(!conns.containsKey(scope)) 
			return null;
		Object value = conns.get(scope);
		if(value instanceof Connection) 
			return (Connection) value;
		List scopeConns = (List) value;
		// First we look for a persistent connection
		// We will search in order (newer connections at the top of the list)
		Iterator it = scopeConns.iterator();
		while(it.hasNext()){
			Connection conn = (Connection) it.next();
			if(conn.getType() == Connection.PERSISTENT) 
				return conn;
		}
		// Ok so no persistent connection
		// lets check to see if we have polling (next best thing)
		it = scopeConns.iterator();
		while(it.hasNext()){
			Connection conn = (Connection) it.next();
			if(conn.getType() == Connection.POLLING) 
				return conn;
		}
		// Looks like they are all transient connections, 
		// return the most recent one
		return (Connection) scopeConns.get(0);
	}
	*/
	
	public boolean equals(Object obj) {
		if(!(obj instanceof Client)) return false;
		final Client client = (Client) obj;
		return client.getId() == getId();
 	}

	public String toString() {
		return "Client: "+id;
	}
	
	void register(Connection conn){
		scopeToConnMap.put(conn.getScope(), conn);
	}
	
	void unregister(Connection conn){
		scopeToConnMap.remove(conn.getScope());
	}
		
}