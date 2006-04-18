package org.red5.server.so;


public class SharedObjectEvent 
	implements ISharedObjectEvent {

	private Type type;
	private String key;
	private Object value;
	
	public SharedObjectEvent(Type type, String key, Object value){
		this.type = type;
		this.key = key;
		this.value = value;
	}
	
	public String getKey() {
		return key;
	}

	public Type getType() {
		return type;
	}

	public Object getValue() {
		return value;
	}
	
}
