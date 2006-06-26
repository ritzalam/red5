package org.red5.server.so;

import java.util.List;

import org.red5.server.net.rtmp.event.IRTMPEvent;

public interface ISharedObjectMessage extends IRTMPEvent {

	/**
	 * Returns the name of the shared object this message belongs to.
	 * 
	 * @return name of the shared object
	 */
	public String getName();
	
	/**
	 * Returns the version to modify.
	 *  
	 * @return version to modify
	 */
	public int getVersion();
	
	/**
	 * Does the message affect a persistent shared object? 
	 * 
	 * @return true if a persistent shared object should be updated otherwise false
	 */
	public boolean isPersistent();
	
	/**
	 * Returns a set of ISharedObjectEvent objects containing informations what to change.
	 *  
	 * @return set of ISharedObjectEvents
	 */
	public List<ISharedObjectEvent> getEvents();

	public void addEvent(ISharedObjectEvent.Type type, String key, Object value);
	
	public void addEvent(ISharedObjectEvent event);
	
	public void clear();
	
	public boolean isEmpty(); 
	
}
