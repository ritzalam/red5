package org.red5.server;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.red5.server.api.IAttributeStore;
import org.red5.server.api.ICastingAttributeStore;

public class AttributeStore implements IAttributeStore, ICastingAttributeStore {

	protected Map<String,Object> attributes = new HashMap<String,Object>();
	
	public AttributeStore() {
		// Object is not associated with a persistence storage
	}
	
	public Set<String> getAttributeNames(){
		return attributes.keySet();
	}

	public Object getAttribute(String name) {
		return attributes.get(name);
	}

	synchronized public Object getAttribute(String name, Object defaultValue) {
		if (!hasAttribute(name))
			setAttribute(name, defaultValue);
		
		return getAttribute(name);
	}

	public boolean hasAttribute(String name) {
		return attributes.containsKey(name);
	}
	
	synchronized public boolean setAttribute(String name, Object value) {
		if (name == null)
			return false;
		
		Object old = attributes.get(name);
		if ((old == null && value != null) || !old.equals(value)) {
			// Attribute value changed
			attributes.put(name,value);
			return true;
		} else
			return false;
	}

	synchronized public void setAttributes(Map<String,Object> values) {
		attributes.putAll(values);
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
		if (name == null)
			return false;
		
		boolean result = hasAttribute(name);
		attributes.remove(name);
		return result;
	}
	
	synchronized public void removeAttributes() {
		attributes.clear();
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
