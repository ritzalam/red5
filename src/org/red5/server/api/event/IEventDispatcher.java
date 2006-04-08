package org.red5.server.api.event;

public interface IEventDispatcher {

	public void dispatchEvent(Object event);
	
	public void dispatchEvent(IEvent event);
	
}