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

package org.red5.server;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.management.openmbean.CompositeData;

import org.red5.server.api.IAttributeStore;
import org.red5.server.api.ICastingAttributeStore;

public class AttributeStore implements ICastingAttributeStore {

	/**
	 * Map for attributes
	 */
	protected ConcurrentMap<String, Object> attributes = new ConcurrentHashMap<String, Object>(1);

	/**
	 * Filter <code>null</code> keys and values from given map.
	 * 
	 * @param values		the map to filter
	 * @return filtered map
	 */
	protected Map<String, Object> filterNull(Map<String, Object> values) {
		Map<String, Object> result = new HashMap<String, Object>();
		for (Map.Entry<String, Object> entry : values.entrySet()) {
			String key = entry.getKey();
			if (key == null) {
				continue;
			}
			Object value = entry.getValue();
			if (value == null) {
				continue;
			}
			result.put(key, value);
		}
		return result;
	}

	/**
	 * Creates empty attribute store. Object is not associated with a persistence storage.
	 */
	public AttributeStore() {
	}

	/**
	 * Creates attribute store with initial values. Object is not associated with a persistence storage.
	 * @param values map
	 */
	public AttributeStore(Map<String, Object> values) {
		setAttributes(values);
	}

	/**
	 * Creates attribute store with initial values. Object is not associated with a persistence storage.
	 * @param values map
	 */
	public AttributeStore(IAttributeStore values) {
		setAttributes(values);
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
		if (name == null) {
			return null;
		}
		return attributes.get(name);
	}

	/**
	 * Return the value for a given attribute and set it if it doesn't exist.
	 *
	 * @param name         the name of the attribute to get
	 * @param defaultValue the value of the attribute to set if the attribute doesn't
	 *                     exist
	 * @return the attribute value
	 */
	public Object getAttribute(String name, Object defaultValue) {
		if (name == null) {
			return null;
		}
		if (defaultValue == null) {
			throw new NullPointerException("the default value may not be null");
		}
		Object result = attributes.putIfAbsent(name, defaultValue);
		// if no previous value result will be null
		if (result == null) {
			// use the default value
			result = defaultValue;
		}
		return result;
	}

	/**
	 * Check the object has an attribute.
	 *
	 * @param name the name of the attribute to check
	 * @return true if the attribute exists otherwise false
	 */
	public boolean hasAttribute(String name) {
		if (name == null) {
			return false;
		}
		return attributes.containsKey(name);
	}

	/**
	 * Set an attribute on this object.
	 *
	 * @param name  the name of the attribute to change
	 * @param value the new value of the attribute
	 * @return true if the attribute value was added or changed, otherwise false
	 */
	public boolean setAttribute(String name, Object value) {
		if (name != null) {
			if (value != null) {
				// update with new value
				Object previous = attributes.put(name, value);
				// previous will be null if the attribute didn't exist
				return (previous == null || !value.equals(previous));
			}
		}
		return false;
	}

	/**
	 * Set multiple attributes on this object.
	 *
	 * @param values the attributes to set
	 */
	public void setAttributes(Map<String, Object> values) {
		attributes.putAll(filterNull(values));
	}

	/**
	 * Set multiple attributes on this object.
	 *
	 * @param values the attributes to set
	 */
	public void setAttributes(IAttributeStore values) {
		setAttributes(values.getAttributes());
	}

	/**
	 * Remove an attribute.
	 *
	 * @param name the name of the attribute to remove
	 * @return true if the attribute was found and removed otherwise false
	 */
	public boolean removeAttribute(String name) {
		if (name != null) {
			return (attributes.remove(name) != null);
		}
		return false;
	}

	/**
	 * Remove all attributes.
	 */
	public void removeAttributes() {
		attributes.clear();
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
	public List<?> getListAttribute(String name) {
		return (List<?>) getAttribute(name);
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
	public Map<?, ?> getMapAttribute(String name) {
		return (Map<?, ?>) getAttribute(name);
	}

	/**
	 * Get Set attribute by name
	 *
	 * @param name Attribute name
	 * @return Attribute
	 */
	public Set<?> getSetAttribute(String name) {
		return (Set<?>) getAttribute(name);
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

	/**
	 * Allows for reconstruction via CompositeData.
	 * 
	 * @param cd composite data
	 * @return AttributeStore class instance
	 */
	@SuppressWarnings("unchecked")
	public static AttributeStore from(CompositeData cd) {
		AttributeStore instance = null;
		if (cd.containsKey("attributes")) {
			Object cn = cd.get("attributes");
			if (cn != null) {
				if (cn instanceof IAttributeStore) {
					instance = new AttributeStore((IAttributeStore) cn);
				} else if (cn instanceof Map) {
					instance = new AttributeStore((Map<String, Object>) cn);
				}
			} else {
				instance = new AttributeStore();
			}
		} else {
			instance = new AttributeStore();
		}
		return instance;
	}

}
