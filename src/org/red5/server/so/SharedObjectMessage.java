package org.red5.server.so;

import java.util.Iterator;
import java.util.LinkedList;

import org.red5.server.api.event.IEventListener;

public class SharedObjectMessage implements ISharedObjectMessage {

	private IEventListener source;
	private String name;
	private LinkedList<ISharedObjectEvent> events = new LinkedList<ISharedObjectEvent>();
	private int version = 0;
	private boolean persistent = false;
	
	public SharedObjectMessage(IEventListener source, String name, int version, boolean persistent){
		this.source = source;
		this.name = name;
		this.version = version;
		this.persistent = persistent;
	}
	
	public int getVersion() {
		return version;
	}

	public String getName() {
		return name;
	}
	
	public boolean isPersistent() {
		return persistent;
	}
	
	public void addEvent(ISharedObjectEvent event){
		events.add(event);
	}
	
	public LinkedList<ISharedObjectEvent> getEvents(){
		return events;
	}

	public void addEvent(ISharedObjectEvent.Type type, String key, Object value) {
		events.add(new SharedObjectEvent(type, key, value));
	}

	public void clear() {
		events.clear();
	}

	public boolean isEmpty() {
		return events.isEmpty();
	}

	public Type getType() {
		return Type.SHARED_OBJECT;
	}

	public Object getObject() {
		return getEvents();
	}

	public boolean hasSource() {
		return source != null;
	}

	public IEventListener getSource() {
		return source;
	}

	public String toString(){
		final StringBuffer sb = new StringBuffer();
		sb.append("SharedObjectMessage: ").append(name).append(" { ");
		final Iterator it = events.iterator();
		while(it.hasNext()){
			sb.append(it.next());
			if(it.hasNext()) sb.append(" , ");
		}
		sb.append(" } ");
		return sb.toString();
	}
	
}
