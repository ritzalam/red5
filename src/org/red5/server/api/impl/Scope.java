package org.red5.server.api.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections.set.UnmodifiableSet;
import org.red5.server.api.BroadcastStream;
import org.red5.server.api.Connection;
import org.red5.server.api.ScopeHandler;
import org.red5.server.api.SharedObject;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

public class Scope extends AttributeStore 
	implements org.red5.server.api.Scope {
	
	private Scope parent;
	private String contextPath = "";
	private ScopeHandler handler;
	private ApplicationContext context;
	
	private HashMap childScopes = new HashMap();

	private HashMap broadcastStreams = new HashMap();
	private HashMap sharedObjects = new HashMap();
	private HashSet clients = new HashSet();
	
	public Scope(Scope parent, String contextPath, ScopeHandler handler, ApplicationContext context){
		this.parent = parent;
		this.contextPath = contextPath;
		this.handler = handler;
		this.context = context;
	}
	
	public void dispatchEvent(Object event) {
		// TODO: Should this be in a util class ?
	}
	
	public BroadcastStream getBroadcastStream(String name) {
		return (BroadcastStream) broadcastStreams.get(name);
	}

	public Set getBroadcastStreamNames() {
		return broadcastStreams.keySet();
	}

	public boolean hasChildScope(String name){
		return childScopes.containsKey(name);
	}
	
	public org.red5.server.api.Scope getChildScope(String name) {
		return (org.red5.server.api.Scope) childScopes.get(name);
	}

	public Set getChildScopeNames() {
		return childScopes.keySet();
	}

	public Set getClients() {
		return UnmodifiableSet.decorate(clients);
	}

	public ApplicationContext getContext() {
		return context;
	}

	public String getContextPath() {
		return contextPath;
	}

	public ScopeHandler getHandler() {
		return handler;
	}

	public org.red5.server.api.Scope getParent() {
		return parent;
	}

	public Resource getResource(String path) {
		return context.getResource(contextPath + '/' + path);
	}

	public Resource[] getResources(String pattern) throws IOException {
		return context.getResources(contextPath + '/' + pattern);
	}

	public boolean createSharedObject(String name, boolean persistent){
		// TODO:
		return false;
	}
	
	public SharedObject getSharedObject(String name) {
		return (SharedObject) sharedObjects.get(name);
	}

	public Set getSharedObjectNames() {
		return sharedObjects.keySet();
	}

	public boolean hasBroadcastStream(String name) {
		return broadcastStreams.containsKey(name);
	}

	public boolean hasParent() {
		return (parent != null);
	}

	boolean connect(Connection conn) {
		if(!handler.canConnect(conn)) return false;
		if(!clients.contains(conn.getClient())){
			clients.add(conn.getClient());
			handler.onConnect(conn);		
		}
		return true;
	}
	
	void disconnect(Connection conn){
		if(clients.contains(conn.getClient())){
			clients.remove(conn.getClient());
			handler.onDisconnect(conn);
		}
	}
	
}