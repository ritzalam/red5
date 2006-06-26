package org.red5.server.net.rtmp.event;

import org.red5.server.api.event.IEventListener;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.Header;

public abstract class BaseEvent implements Constants, IRTMPEvent {
	
	private Type type;
	protected Object object;
	protected IEventListener source;
	protected int timestamp;
	protected Header header = null;
	
	public BaseEvent(Type type) {
		this(type, null);
	}
	
	public BaseEvent(Type type, IEventListener source) {
		this.type = type;
		this.source = source;
	}
	
	public Type getType() {
		return type;
	}
	
	public Object getObject() {
		return object;
	}
	
	public Header getHeader() {
		return header;
	}
	
	public void setHeader(Header header) {
		this.header = header;
	}
	
	public boolean hasSource() {
		return source != null;
	}
	
	public IEventListener getSource() {
		return source;
	}

	public abstract byte getDataType();
	
	public int getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}
	
	public void release() {
		
	}
}
