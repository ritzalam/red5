package org.red5.server.net.rtmp.message;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.red5.server.so.ISharedObjectEvent;


public class SharedObject extends Unknown {

	private String name;
	private LinkedList<SharedObjectEvent> events = new LinkedList<SharedObjectEvent>();
	private int timer;
	private int soId = 0;
	private int type = 0;

	public SharedObject(){
		super(TYPE_SHARED_OBJECT);
	}
	
	public int getSoId() {
		return soId;
	}

	public void setSoId(int soId) {
		this.soId = soId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setType(int type) {
		this.type = type;
	}
	
	public int getType() {
		return this.type;
	}
	
	public boolean isPersistent() {
		// TODO: what do the values mean?
		return this.type != 0;
	}
	
	public void addEvent(SharedObjectEvent event){
		events.add(event);
	}

	public void addEvents(List<SharedObjectEvent> events){
		this.events.addAll(events);
	}

	public LinkedList<SharedObjectEvent> getEvents(){
		return events;
	}

	public int getTimer() {
		return timer;
	}

	public void setTimer(int timer) {
		this.timer = timer;
	}

	public String toString(){
		final StringBuffer sb = new StringBuffer();
		sb.append("SharedObject: ").append(name).append(" { ");
		final Iterator it = events.iterator();
		while(it.hasNext()){
			sb.append(it.next());
			if(it.hasNext()) sb.append(" , ");
		}
		sb.append(" } ").append(getData().getHexDump());
		return sb.toString();
	}
	
}
