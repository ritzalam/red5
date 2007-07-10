package org.red5.compatibility.flex.messaging.io;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 *
 * Copyright (c) 2006-2007 by respective authors (see below). All rights reserved.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.red5.io.amf3.IDataInput;
import org.red5.io.amf3.IDataOutput;
import org.red5.io.amf3.IExternalizable;

/**
 * Flex <code>ObjectProxy</code> compatibility class.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class ObjectProxy implements IExternalizable {

	/** The proxied object. */
	private Map<Object, Object> item;
	
	/** Create new empty proxy. */
	public ObjectProxy() {
		this(new HashMap<Object, Object>());
	}
	
	/**
	 * Create proxy for given object.
	 * 
	 * @param item object to proxy
	 */
	public ObjectProxy(Map<Object, Object> item) {
		this.item = new HashMap<Object, Object>(item);
	}

	/** {@inheritDoc} */
	@SuppressWarnings("unchecked")
	public void readExternal(IDataInput input) {
		item = (Map<Object, Object>) input.readObject();
	}

	/** {@inheritDoc} */
	public void writeExternal(IDataOutput output) {
		output.writeObject(item);
	}

	/**
	 * Provide access to proxied object. All properties of the
	 * proxied object are read-only.
	 * 
	 * @return the proxied object
	 */
	@SuppressWarnings("unchecked")
	public Map getItem() {
		return Collections.unmodifiableMap(item);
	}
	
	/**
	 * Change a property of the proxied object.
	 * 
	 * @param name
	 * @param value
	 */
	public void setProperty(Object name, Object value) {
		item.put(name, value);
	}
	
	/**
	 * Return the value of a property.
	 * 
	 * @param name
	 */
	public Object getProperty(Object name) {
		return item.get(name);
	}

	/**
	 * Remove a property of the proxied object.
	 * 
	 * @param name
	 */
	public void deleteProperty(Object name) {
		item.remove(name);
	}
	
	/**
	 * Check if proxied object has a given property.
	 * 
	 * @param name
	 * @return
	 */
	public boolean hasProperty(Object name) {
		return item.containsKey(name);
	}
	
	/** {@inheritDoc} */
	public String toString() {
		return item.toString();
	}
	
	// TODO: implement other ObjectProxy methods
	
}
