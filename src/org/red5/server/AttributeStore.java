package org.red5.server;

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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.red5.server.api.IAttributeStore;
import org.red5.server.api.ICastingAttributeStore;

public class AttributeStore implements ICastingAttributeStore {

    /**
     * Map for attributes
     */
    protected Map<String, Object> attributes = new ConcurrentHashMap<String, Object>();
    /**
     * Map for hashes
     */
	protected Map<String, Integer> hashes = new HashMap<String, Integer>();

    /**
     * Creates attribute store. Object is not associated with a persistence storage.
     */
    public AttributeStore() {

	}

    /**
     * Get the attribute names. The resulting set will be read-only.
     *
     * @return set containing all attribute names
     */
    public Set<String> getAttributeNames() {
        return Collections.unmodifiableSet(attributes.keySet());
    }

    /**
     * Get the attributes. The resulting map will be read-only.
     *
     * @return map containing all attributes
     */
    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    /**
     * Return the value for a given attribute.
     *
     * @param name the name of the attribute to get
     * @return the attribute value or null if the attribute doesn't exist
     */
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    /**
     * Return the value for a given attribute and set it if it doesn't exist.
     * <p/>
     * <p/>
     * This is a utility function that internally performs the following code:
     * <p/>
     * <code>
     * if (!hasAttribute(name)) setAttribute(name, defaultValue);<br>
     * return getAttribute(name);<br>
     * </code>
     * </p>
     * </p>
     *
     * @param name         the name of the attribute to get
     * @param defaultValue the value of the attribute to set if the attribute doesn't
     *                     exist
     * @return the attribute value
     */
    public Object getAttribute(String name, Object defaultValue) {
    	synchronized (attributes) {
    		if (!hasAttribute(name)) {
    			setAttribute(name, defaultValue);
    		}
    	}

        return getAttribute(name);
    }

    /**
     * Check the object has an attribute.
     *
     * @param name the name of the attribute to check
     * @return true if the attribute exists otherwise false
     */
    public boolean hasAttribute(String name) {
        return attributes.containsKey(name);
    }

    /**
     * Set an attribute on this object.
     *
     * @param name  the name of the attribute to change
     * @param value the new value of the attribute
     * @return true if the attribute value changed otherwise false
     */
    public boolean setAttribute(String name, Object value) {
        if (name == null) {
            return false;
        }

        synchronized (attributes) {
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
    }

    /**
     * Set multiple attributes on this object.
     *
     * @param values the attributes to set
     */
    public void setAttributes(Map<String, Object> values) {
    	synchronized (attributes) {
	        attributes.putAll(values);
	        hashes.clear();
	        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
	            Object value = entry.getValue();
	            hashes.put(entry.getKey(), value != null ? value.hashCode() : 0);
	        }
    	}
    }

    /**
     * Set multiple attributes on this object.
     *
     * @param values the attributes to set
     */
    public void setAttributes(IAttributeStore values) {
    	synchronized (attributes) {
    		Iterator it = values.getAttributeNames().iterator();
	        while (it.hasNext()) {
	            String name = (String) it.next();
	            Object value = values.getAttribute(name);
	            setAttribute(name, value);
	        }
    	}
    }

    /**
     * Remove an attribute.
     *
     * @param name the name of the attribute to remove
     * @return true if the attribute was found and removed otherwise false
     */
    public boolean removeAttribute(String name) {
        if (name == null) {
            return false;
        }

        synchronized (attributes) {
	        boolean result = hasAttribute(name);
	        attributes.remove(name);
	        hashes.remove(name);
	        return result;
        }
    }

    /**
     * Remove all attributes.
     */
    public void removeAttributes() {
    	synchronized (attributes) {
    		attributes.clear();
    		hashes.clear();
    	}
    }

    /**
     * Get Boolean attribute by name
     *
     * @param name Attribute name
     * @return Attribute
     */
    public Boolean getBoolAttribute(String name) {
        return (Boolean) getAttribute(name);
    }

    /**
     * Get Byte attribute by name
     *
     * @param name Attribute name
     * @return Attribute
     */
    public Byte getByteAttribute(String name) {
        return (Byte) getAttribute(name);
    }

    /**
     * Get Double attribute by name
     *
     * @param name Attribute name
     * @return Attribute
     */
    public Double getDoubleAttribute(String name) {
        return (Double) getAttribute(name);
    }

    /**
     * Get Integer attribute by name
     *
     * @param name Attribute name
     * @return Attribute
     */
    public Integer getIntAttribute(String name) {
        return (Integer) getAttribute(name);
    }

    /**
     * Get List attribute by name
     *
     * @param name Attribute name
     * @return Attribute
     */
    public List getListAttribute(String name) {
        return (List) getAttribute(name);
    }

    /**
     * Get boolean attribute by name
     *
     * @param name Attribute name
     * @return Attribute
     */
    public Long getLongAttribute(String name) {
        return (Long) getAttribute(name);
    }

    /**
     * Get Long attribute by name
     *
     * @param name Attribute name
     * @return Attribute
     */
    public Map getMapAttribute(String name) {
        return (Map) getAttribute(name);
    }

    /**
     * Get Set attribute by name
     *
     * @param name Attribute name
     * @return Attribute
     */
    public Set getSetAttribute(String name) {
        return (Set) getAttribute(name);
    }

    /**
     * Get Short attribute by name
     *
     * @param name Attribute name
     * @return Attribute
     */
    public Short getShortAttribute(String name) {
        return (Short) getAttribute(name);
    }

    /**
     * Get String attribute by name
     *
     * @param name Attribute name
     * @return Attribute
     */
    public String getStringAttribute(String name) {
        return (String) getAttribute(name);
    }
}
