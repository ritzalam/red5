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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.red5.server.api.IAttributeStore;
import org.red5.server.api.ICastingAttributeStore;

public class AttributeStore implements ICastingAttributeStore {

	protected Map<String, Object> attributes = new ConcurrentHashMap<String, Object>();

	protected Map<String, Integer> hashes = new HashMap<String, Integer>();

	public AttributeStore() {
		// Object is not associated with a persistence storage
	}

	public Set<String> getAttributeNames() {
		return Collections.unmodifiableSet(attributes.keySet());
	}

	public Map<String, Object> getAttributes() {
		return Collections.unmodifiableMap(attributes);
	}
	
	public Object getAttribute(String name) {
		return attributes.get(name);
	}

	synchronized public Object getAttribute(String name, Object defaultValue) {
		if (!hasAttribute(name)) {
			setAttribute(name, defaultValue);
		}

		return getAttribute(name);
	}

	public boolean hasAttribute(String name) {
		return attributes.containsKey(name);
	}

	synchronized public boolean setAttribute(String name, Object value) {
		if (name == null) {
			return false;
		}

		Object old = attributes.get(name);
		Integer newHash = (value != null ? value.hashCode() : 0);
		if ((old == null && value != null)
				|| (old != null && !old.equals(value))
				|| !newHash.equals(hashes.get(name))) {
			// Attribute value changed
			attributes.put(name, value);
			hashes.put(name, newHash);
			return true;
		} else {
			return false;
		}
	}

	synchronized public void setAttributes(Map<String, Object> values) {
		attributes.putAll(values);
		hashes.clear();
		for (Map.Entry<String, Object> entry : attributes.entrySet()) {
			Object value = entry.getValue();
			hashes.put(entry.getKey(), value != null ? value.hashCode() : 0);
		}
	}

	synchronized public void setAttributes(IAttributeStore values) {
		Iterator it = values.getAttributeNames().iterator();
		while (it.hasNext()) {
			String name = (String) it.next();
			Object value = values.getAttribute(name);
			setAttribute(name, value);
		}
	}

	synchronized public boolean removeAttribute(String name) {
		if (name == null) {
			return false;
		}

		boolean result = hasAttribute(name);
		attributes.remove(name);
		hashes.remove(name);
		return result;
	}

	synchronized public void removeAttributes() {
		attributes.clear();
		hashes.clear();
	}

	public Boolean getBoolAttribute(String name) {
		return (Boolean) getAttribute(name);
	}

	public Byte getByteAttribute(String name) {
		return (Byte) getAttribute(name);
	}

	public Double getDoubleAttribute(String name) {
		return (Double) getAttribute(name);
	}

	public Integer getIntAttribute(String name) {
		return (Integer) getAttribute(name);
	}

	public List getListAttribute(String name) {
		return (List) getAttribute(name);
	}

	public Long getLongAttribute(String name) {
		return (Long) getAttribute(name);
	}

	public Map getMapAttribute(String name) {
		return (Map) getAttribute(name);
	}

	public Set getSetAttribute(String name) {
		return (Set) getAttribute(name);
	}

	public Short getShortAttribute(String name) {
		return (Short) getAttribute(name);
	}

	public String getStringAttribute(String name) {
		return (String) getAttribute(name);
	}
}
