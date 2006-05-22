package org.red5.server.api;

import org.red5.server.api.event.IEventObservable;
import org.red5.server.api.persistence.IPersistable;

public interface IBasicScope 
	extends ICoreObject, 
		IEventObservable, 
		Iterable<IBasicScope>,
		IPersistable {
	
	/**
	 * Does this scope have a parent
	 * 
	 * @return true if this scope has a parent
	 */
	public boolean hasParent();

	/**
	 * Get this scopes parent
	 * 
	 * @return parent scope, or null if this scope doesn't have a parent
	 */
	public IScope getParent();

	/**
	 * Get the scopes depth, how far down the scope tree is it
	 * 
	 * @return depth
	 */
	public int getDepth();

	/**
	 * Get the name of this scope. eg. someroom
	 * 
	 * @return name
	 */
	public String getName();

	/**
	 * Get the full absolute path. eg. host/myapp/someroom
	 * 
	 * @return path
	 */
	public String getPath();
	
	public boolean isPersistent();
	
	public void setPersistent(boolean persistent);
	
	public String getType();
	
}
