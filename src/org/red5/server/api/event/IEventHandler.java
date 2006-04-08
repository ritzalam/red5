package org.red5.server.api.event;

public interface IEventHandler {

	/**
	 * Handle an event
	 * @param event event to handle
	 * @return true if event was handled, false if it should bubble
	 */
	public boolean handleEvent(IEvent event);
	
}