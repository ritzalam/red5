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


/**
 * Basic attribute store implemetation
 */
public class AttributeStore implements ICastingAttributeStore {

	protected Map<String, Object> attributes = new ConcurrentHashMap<String, Object>();
	
	protected Map<String, Integer> hashes = new HashMap<String, Integer>();

    
    public AttributeStore() {
		// Object is not associated with a persistence storage
	}
	
	
	/**
	 * Returns attribute names as Set.
     *
	 * @return      Set of store attributes
	 * @see org.red5.server.api.IAttributeStore#getAttributeNames()
	 */
	public Set<String> getAttributeNames() {
		return Collections.unmodifiableSet(attributes.keySet());
	}

    /**
     * Return all attributes
     *
     * @return                 Map containing attributes
     */
	public Map<String, Object> getAttributes() {
		return Collections.unmodifiableMap(attributes);
	}
	
    /**
     * Return attribute by name
     *
     * @param name             Attribute name
     * @return                 Attribute value
     */
	public Object getAttribute(String name) {
		return attributes.get(name);
	}

    /**
     * Sets attribute and reterns new value immediately. Thread safe 
     *
     * @param name                 Attribute name
     * @param defaultValue         Attribute value to set
     * @return                     New attribute value
     */
    synchronized public Object getAttribute(String name, Object defaultValue) {
		if (!hasAttribute(name)) {
			setAttribute(name, defaultValue);
		}

		return getAttribute(name);
	}

    /**
     * Check whether attributes store has attribute with given name
     *
     * @param name        Attribute name
     * @return            <code>true</code> if attribute store has attribute with given name, <code>false</code> otherwise
     */
    public boolean hasAttribute(String name) {
		return attributes.containsKey(name);
	}

    /**
     * Sets attribute value and returns success as boolean
     *
     * @param name        Attribute name
     * @param value       Attribute value
     * @return            true if attribute was set, false otherwise
     */
    synchronized public boolean setAttribute(String name, Object value) {
		if (name == null) {
			return false;
		}

        // Get old attribute and check if it has been changed
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

    /**
     * Sets attributes from Map
     *
     * @param values        Attributes to set
     */
    synchronized public void setAttributes(Map<String, Object> values) {
		attributes.putAll(values);
		hashes.clear();
		for (Map.Entry<String, Object> entry : attributes.entrySet()) {
			Object value = entry.getValue();
			hashes.put(entry.getKey(), value != null ? value.hashCode() : 0);
		}
	}

    /**
     * Bulk set attributes from another attributes store
     *
     * @param values      Attributes store
     */
    synchronized public void setAttributes(IAttributeStore values) {
		Iterator it = values.getAttributeNames().iterator();
		while (it.hasNext()) {
			String name = (String) it.next();
			Object value = values.getAttribute(name);
			setAttribute(name, value);
		}
	}

    /**
     * Removes attribute
     *
     * @param name      Attribute name
     * @return          Whether attribute was removed or not
     */
    synchronized public boolean removeAttribute(String name) {
		if (name == null) {
			return false;
		}

		boolean result = hasAttribute(name);
		attributes.remove(name);
		hashes.remove(name);
		return result;
	}

    /**
     * Clears all attributes
     */
    synchronized public void removeAttributes() {
		attributes.clear();
		hashes.clear();
	}

    /**
     * Return attribute casted to Boolean
     *
     * @param name     Attribute name
     * @return         Attribute value casted to Boolean
     */
    public Boolean getBoolAttribute(String name) {
		return (Boolean) getAttribute(name);
	}

    /**
     * Return attribute casted to Byte
     *
     * @param name     Attribute name
     * @return         Attribute value casted to Byte
     */
    public Byte getByteAttribute(String name) {
		return (Byte) getAttribute(name);
	}

    /**
     * Return attribute casted to Double
     *
     * @param name     Attribute name
     * @return         Attribute value casted to Double
     */
    public Double getDoubleAttribute(String name) {
		return (Double) getAttribute(name);
	}

    /**
     * Return attribute casted to Integer
     *
     * @param name     Attribute name
     * @return         Attribute value casted to Integer
     */
    public Integer getIntAttribute(String name) {
		return (Integer) getAttribute(name);
	}

    /**
     * Return attribute casted to List
     *
     * @param name     Attribute name
     * @return         Attribute value casted to List
     */
    public List getListAttribute(String name) {
		return (List) getAttribute(name);
	}

    /**
     * Return attribute casted to Long
     *
     * @param name     Attribute name
     * @return         Attribute value casted to Long
     */
    public Long getLongAttribute(String name) {
		return (Long) getAttribute(name);
	}

    /**
     * Return attribute casted to Map
     *
     * @param name     Attribute name
     * @return         Attribute value casted to Map
     */
    public Map getMapAttribute(String name) {
		return (Map) getAttribute(name);
	}

    /**
     * Return attribute casted to Set
     *
     * @param name     Attribute name
     * @return         Attribute value casted to Set
     */
    public Set getSetAttribute(String name) {
		return (Set) getAttribute(name);
	}

    /**
     * Return attribute casted to Short
     *
     * @param name     Attribute name
     * @return         Attribute value casted to Short
     */
    public Short getShortAttribute(String name) {
		return (Short) getAttribute(name);
	}

    /**
     * Return attribute casted to String
     *
     * @param name     Attribute name
     * @return         Attribute value casted to String
     */
    public String getStringAttribute(String name) {
		return (String) getAttribute(name);
	}
}
