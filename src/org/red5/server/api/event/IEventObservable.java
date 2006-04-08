package org.red5.server.api.event;

import java.util.Iterator;

public interface IEventObservable {

	public void addEventListener(IEventListener listener);
	public void removeEventListener(IEventListener listener);
	public Iterator<IEventListener> getEventListeners();
	
}
