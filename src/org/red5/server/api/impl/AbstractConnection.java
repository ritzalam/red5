package org.red5.server.api.impl;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections.set.UnmodifiableSet;
import org.red5.server.api.Client;
import org.red5.server.api.Stream;

public abstract class AbstractConnection extends AttributeStore 
	implements org.red5.server.api.Connection {
	
	private Client client = null;
	private Scope scope = null;
	private HashSet streams = new HashSet();

	public AbstractConnection(Client client, Scope scope){
		this.client = client;
		this.scope = scope;
	}
	
	public abstract void close();

	public abstract void dispatchEvent(Object object);

	public Client getClient() {
		return client;
	}

	public org.red5.server.api.Scope getScope() {
		return scope;
	}

	public Set getStreams() {
		return UnmodifiableSet.decorate(streams);
	}

	public abstract String getType();

	public abstract boolean isConnected();

	public boolean switchScope(String contextPath) {
		// At the moment this method is not dealing with tree schematics
		Scope newScope = (Scope) ScopeUtils.resolveScope(scope, contextPath);
		if(newScope == null) return false;
		if(newScope.connect(this)){
			scope.disconnect(this);
			scope = newScope;
			return true;
		} else 	return false;
	}
	
	void register(Stream stream){
		streams.add(stream);
	}
	
	void unregister(Stream stream){
		if(streams.contains(stream))
			streams.remove(stream);
	}

}