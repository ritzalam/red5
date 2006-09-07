package org.red5.server;

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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.red5.io.object.Deserializer;
import org.red5.io.object.Input;
import org.red5.io.object.Output;
import org.red5.io.object.Serializer;
import org.red5.server.api.IAttributeStore;
import org.red5.server.api.persistence.IPersistable;
import org.red5.server.api.persistence.IPersistenceStore;

public class PersistableAttributeStore extends AttributeStore 
	implements IPersistable {
	
	protected boolean persistent = true;
	protected String name;
	protected String type;
	protected String path;
	protected long lastModified = -1;
	protected IPersistenceStore store = null;
	
	public PersistableAttributeStore(String type, String name, String path, boolean persistent){
		this.type = type;
		this.name = name;
		this.path = path;
		this.persistent = persistent;
	}
	
	protected void modified(){
		lastModified = System.currentTimeMillis();
		if (store != null)
			store.save(this);
	}
	
	public boolean isPersistent() {
		return persistent;
	}

	public void setPersistent(boolean persistent) {
		this.persistent = persistent;
	}

	public long getLastModified() {
		return lastModified;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
	
	public String getType() {
		return type;
	}

	public void serialize(Output output) throws IOException {
		Serializer serializer = new Serializer();
		Map<String, Object> persistentAttributes = new HashMap<String, Object>();
		for (String name : attributes.keySet()) {
			if (name.startsWith(IPersistable.TRANSIENT_PREFIX))
				continue;
			
			persistentAttributes.put(name, attributes.get(name));
		}
		serializer.serialize(output, persistentAttributes);
	}

	public void deserialize(Input input) throws IOException {
		Deserializer deserializer = new Deserializer();
		Object obj = deserializer.deserialize(input);
		if (!(obj instanceof Map))
			throw new IOException("required Map object");
		
		attributes.putAll((Map<String, Object>) obj);
	}

	public void setStore(IPersistenceStore store) {
		this.store = store;
		if (store != null)
			store.load(this);
	}
	
	public IPersistenceStore getStore() {
		return store;
	}
	
	synchronized public boolean setAttribute(String name, Object value) {
		boolean result = super.setAttribute(name, value);
		if (result && !name.startsWith(IPersistable.TRANSIENT_PREFIX))
			modified();
		return result;
	}
	
	synchronized public void setAttributes(Map<String,Object> values) {
		super.setAttributes(values);
		modified();
	}
	
	synchronized public void setAttributes(IAttributeStore values) {
		super.setAttributes(values);
		modified();
	}
	
	synchronized public boolean removeAttribute(String name) {
		boolean result = super.removeAttribute(name);
		if (result && !name.startsWith(IPersistable.TRANSIENT_PREFIX))
			modified();
		return result;
	}
	
	synchronized public void removeAttributes() {
		super.removeAttributes();
		modified();
	}
}
