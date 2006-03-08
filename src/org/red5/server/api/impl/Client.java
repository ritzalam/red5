package org.red5.server.api.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.red5.server.api.Connection;
import org.red5.server.api.Scope;
import org.red5.server.api.Session;

public class Client implements  org.red5.server.api.Client {

	protected String id;
	protected String host;
	protected Session session;
	
	protected HashMap conns = new HashMap();
	
	public Client(String id, String host, Session session){
		this.id = id;
		this.host = host;
		this.session = session;
	}

	public String getId() {
		return id;
	}
	
	public String getHost() {
		return host;
	}

	public Session getSession() {
		return session;
	}

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

	public boolean equals(Object obj) {
		if(!(obj instanceof Client)) return false;
		final Client client = (Client) obj;
		return client.getId() == getId();
 	}

	public String toString() {
		return "Client: "+id;
	}
		
}
