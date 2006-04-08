package org.red5.server.api.event;

public interface IEvent {
	
	public Type getType();
	public Object getObject();
	public boolean hasSource();
	public IEventListener getSource();
	
	enum Type {
		SYSTEM,
		STATUS,
		SERVICE_CALL,
		SHARED_OBJECT,
		STREAM_CONTROL,
		STREAM_DATA,
		CLIENT,
		SERVER
	}
	
}