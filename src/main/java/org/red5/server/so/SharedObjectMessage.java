/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2012 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.server.so;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.red5.server.api.event.IEventListener;
import org.red5.server.net.rtmp.event.BaseEvent;

/**
 * Shared object event
 */
public class SharedObjectMessage extends BaseEvent implements ISharedObjectMessage {

	private static final long serialVersionUID = -8128704039659990049L;

	/**
	 * SO event name
	 */
	private String name;

	/**
	 * SO events chain
	 */
	private ConcurrentLinkedQueue<ISharedObjectEvent> events = new ConcurrentLinkedQueue<ISharedObjectEvent>();

	/**
	 * SO version, used for synchronization purposes
	 */
	private int version;

	/**
	 * Whether SO persistent
	 */
	private boolean persistent;
	
	public SharedObjectMessage() {
	}

	/**
	 * Creates Shared Object event with given name, version and persistence flag
	 * 
	 * @param name Event name
	 * @param version SO version
	 * @param persistent SO persistence flag
	 */
	public SharedObjectMessage(String name, int version, boolean persistent) {
		this(null, name, version, persistent);
	}

	/**
	 * Creates Shared Object event with given listener, name, SO version and
	 * persistence flag
	 * 
	 * @param source Event listener
	 * @param name Event name
	 * @param version SO version
	 * @param persistent SO persistence flag
	 */
	public SharedObjectMessage(IEventListener source, String name, int version, boolean persistent) {
		super(Type.SHARED_OBJECT, source);
		this.name = name;
		this.version = version;
		this.persistent = persistent;
	}

	/** {@inheritDoc} */
	@Override
	public byte getDataType() {
		return TYPE_SHARED_OBJECT;
	}

	/** {@inheritDoc} */
	public int getVersion() {
		return version;
	}

	/**
	 * Setter for version
	 * 
	 * @param version
	 *            New version
	 */
	protected void setVersion(int version) {
		this.version = version;
	}

	/** {@inheritDoc} */
	public String getName() {
		return name;
	}

	/**
	 * Setter for name
	 * 
	 * @param name
	 *            Event name
	 */
	protected void setName(String name) {
		this.name = name;
	}

	/** {@inheritDoc} */
	public boolean isPersistent() {
		return persistent;
	}

	/**
	 * Setter for persistence flag
	 * 
	 * @param persistent
	 *            Persistence flag
	 */
	protected void setPersistent(boolean persistent) {
		this.persistent = persistent;
	}

	/** {@inheritDoc} */
	public void addEvent(ISharedObjectEvent event) {
		events.add(event);
	}

	public void addEvents(List<ISharedObjectEvent> events) {
		this.events.addAll(events);
	}

	public void addEvents(Queue<ISharedObjectEvent> events) {
		this.events.addAll(events);
	}

	/** {@inheritDoc} */
	public ConcurrentLinkedQueue<ISharedObjectEvent> getEvents() {
		return events;
	}

	/** {@inheritDoc} */
	public void addEvent(ISharedObjectEvent.Type type, String key, Object value) {
		events.add(new SharedObjectEvent(type, key, value));
	}

	/** {@inheritDoc} */
	public void clear() {
		events.clear();
	}

	/** {@inheritDoc} */
	public boolean isEmpty() {
		return events.isEmpty();
	}

	/** {@inheritDoc} */
	@Override
	public Type getType() {
		return Type.SHARED_OBJECT;
	}

	/** {@inheritDoc} */
	@Override
	public Object getObject() {
		return getEvents();
	}

	/** {@inheritDoc} */
	@Override
	protected void releaseInternal() {
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName()).append(": ").append(name).append(" { ");
		final Iterator<ISharedObjectEvent> it = events.iterator();
		while (it.hasNext()) {
			sb.append(it.next());
			if (it.hasNext()) {
				sb.append(" , ");
			}
		}
		sb.append(" } ");
		return sb.toString();
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		name = (String) in.readObject();
		version = in.readInt();
		persistent = in.readBoolean();
		Object o = in.readObject();
		if (o != null && o instanceof ConcurrentLinkedQueue) {
			events = (ConcurrentLinkedQueue<ISharedObjectEvent>) o;
		}
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeObject(name);
		out.writeInt(version);
		out.writeBoolean(persistent);
		out.writeObject(events);
	}

}
