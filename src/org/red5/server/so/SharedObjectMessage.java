package org.red5.server.so;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006 by respective authors (see below). All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either version 2.1 of the License, or (at your option) any later 
 * version. 
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this library; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 */

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.red5.server.api.event.IEventListener;
import org.red5.server.net.rtmp.event.BaseEvent;

public class SharedObjectMessage extends BaseEvent implements ISharedObjectMessage {

	private String name;
	private LinkedList<ISharedObjectEvent> events = new LinkedList<ISharedObjectEvent>();
	private int version = 0;
	private boolean persistent = false;
	
	public SharedObjectMessage(String name, int version, boolean persistent) {
		this(null, name, version, persistent);
	}
	
	public SharedObjectMessage(IEventListener source, String name, int version, boolean persistent){
		super(Type.SHARED_OBJECT, source);
		this.name = name;
		this.version = version;
		this.persistent = persistent;
	}

	public byte getDataType() {
		return TYPE_SHARED_OBJECT; 
	}
	
	public int getVersion() {
		return version;
	}
	
	protected void setVersion(int version) {
		this.version = version;
	}

	public String getName() {
		return name;
	}
	
	protected void setName(String name) {
		this.name = name;
	}
	
	public boolean isPersistent() {
		return persistent;
	}
	
	protected void setIsPersistent(boolean persistent) {
		this.persistent = persistent;
	}
	
	public void addEvent(ISharedObjectEvent event){
		events.add(event);
	}
	
	public void addEvents(List<ISharedObjectEvent> events){
		this.events.addAll(events);
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
