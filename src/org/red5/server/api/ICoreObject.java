package org.red5.server.api;

import org.red5.server.api.event.IEventDispatcher;
import org.red5.server.api.event.IEventHandler;
import org.red5.server.api.event.IEventListener;

public interface ICoreObject 
	extends 
		IAttributeStore, 
		ICastingAttributeStore, 
		IEventDispatcher, 
		IEventHandler, 
		IEventListener {

}