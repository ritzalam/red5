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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.red5.io.amf3.IDataInput;
import org.red5.io.amf3.IDataOutput;
import org.red5.io.amf3.IExternalizable;

/**
 * Flex <code>ObjectProxy</code> compatibility class.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class ObjectProxy implements Map, IExternalizable {

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
	 * Return string representation of the proxied object.
	 * 
	 * @return
	 */
	public String toString() {
		return item.toString();
	}

	public void clear() {
		item.clear();
	}

	/**
	 * Check if proxied object has a given property.
	 * 
	 * @param name
	 * @return
	 */
	public boolean containsKey(Object name) {
		return item.containsKey(name);
	}

	public boolean containsValue(Object value) {
		return item.containsValue(value);
	}

	public Set entrySet() {
		return Collections.unmodifiableSet(item.entrySet());
	}

	/**
	 * Return the value of a property.
	 * 
	 * @param name
	 * @return
	 */
	public Object get(Object name) {
		return item.get(name);
	}

	public boolean isEmpty() {
		return item.isEmpty();
	}

	public Set keySet() {
		return item.keySet();
	}

	/**
	 * Change a property of the proxied object.
	 * 
	 * @param name
	 * @param value
	 * @return
	 */
	public Object put(Object name, Object value) {
		return item.put(name, value);
	}

	@SuppressWarnings("unchecked")
	public void putAll(Map values) {
		item.putAll(values);
	}

	/**
	 * Remove a property from the proxied object.
	 * 
	 * @param name
	 * @return
	 */
	public Object remove(Object name) {
		return item.remove(name);
	}

	public int size() {
		return item.size();
	}

	public Collection values() {
		return Collections.unmodifiableCollection(item.values());
	}
	
	// TODO: implement other ObjectProxy methods
	
}
