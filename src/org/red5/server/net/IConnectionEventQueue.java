package org.red5.server.net;

import java.util.Iterator;

import org.red5.server.api.IConnection;
import org.red5.server.api.event.IEvent;

public interface IConnectionEventQueue {

	public boolean hasEventsWaiting(IConnection conn);
	public Iterator<IEvent> pickupEvents(IConnection conn);
	
}
