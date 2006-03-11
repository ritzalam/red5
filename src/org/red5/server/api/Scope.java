package org.red5.server.api;

import java.io.IOException;
import java.util.Set;

import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

/**
 * The Scope Object
 * A statefull object shared between a group of clients connected to the same context path.
 * Scopes are aranged in a heiracheal way, so its possible for a scope to have a parent.
 * If a client is connect to a scope then they are also connected to its parent scope.
 * The scope object is used to access resources, shared object, streams, etc.
 * The following are all names for scopes: application, room, place, lobby.
 */
public interface Scope extends AttributeStore {

	/**
	 * Does this scope have a parent
	 * 
	 * @return true if this scope has a parent
	 */
	public boolean hasParent();

	/**
	 * Get this scopes parent
	 * 
	 * @return parent scope, or null if this scope doenst have a parent
	 */
	public Scope getParent();

	/**
	 * Get the context path. eg. /myapp/someroom
	 * 
	 * @return context path
	 */
	public String getContextPath();

	/**
	 * Get the spring application context
	 * 
	 * @return context object
	 */
	public ApplicationContext getContext();

	/**
	 * Attempts to load a resource given a path from the context directory
	 * Supports other protocols such as http, see spring docs
	 * 
	 * @param path
	 * 	the path or uri to a resource
	 * @return	resource object
	 */
	public Resource getResource(String path);

	/**
	 * Attempts to resolve a pattern to an array of resources
	 * Works just like getResource but can take ant style patterns
	 * 
	 * @param pattern
	 * 	an ant style pattern
	 * @return	an array of resource objects
	 * @throws IOException
	 */
	public Resource[] getResources(String pattern) throws IOException;

	/**
	 * Check to see if this scope has a child scope matching a given name
	 * 
	 * @param name
	 * 	the name of the child scope
	 * @return	true if a child scope exists, otherwise false
	 */
	public boolean hasChildScope(String name);

	/**
	 * Get a set of the child scope names
	 * 
	 * @return set containing child scope names
	 */
	public Set getChildScopeNames();

	/**
	 * Get a child scope by name
	 * 
	 * @param name
	 * 	name of the child scope
	 * @return the child scope, or null if no scope is found
	 */
	public Scope getChildScope(String name);

	/**
	 * Get a set of the shared object names
	 * 
	 * @return set containing the shared object names
	 */
	public Set getSharedObjectNames();

	/**
	 * Create a new shared object
	 * 
	 * @param name
	 * 	the name of the shared object 
	 * @param peristent
	 * 	will the shared object be persistent
	 * @return	true if the shared object was created, otherwise false
	 */
	public boolean createSharedObject(String name, boolean peristent);
	
	/**
	 * Get a shared object by name
	 * 
	 * @param name
	 * 	the name of the shared object
	 * @return	shared object, or null if not found
	 */
	public SharedObject getSharedObject(String name);

	/**
	 * Get a set of connected clients
	 * You can get the connections by passing the scope to the clients lookupConnection method
	 * 
	 * @return set containing all connected clients
	 */
	public Set getClients();

	/**
	 * Dispatch an event to all connected clients
	 * 
	 * @param event
	 * 	any simple object, which can be serialized and sent to clients
	 */
	public void dispatchEvent(Object event);
	
	/**
	 * Get the scope handler
	 * 
	 * @return scope handler
	 */
	public ScopeHandler getHandler();

	/**
	 * Does the scope have a broadcast stream registered with a given name
	 * 
	 * @param name
	 * 	name of the broadcast
	 * @return true is a stream exists, otherwise false
	 */
	public boolean hasBroadcastStream(String name);

	/**
	 * Get a broadcast stream by name
	 * 
	 * @param name
	 * 	the name of the broadcast
	 * @return broadcast stream object
	 */
	public BroadcastStream getBroadcastStream(String name);

	/**
	 * Get a set containing the names of all the broadcasts
	 * 
	 * @return set containing all broadcast names
	 */
	public Set getBroadcastStreamNames();

}