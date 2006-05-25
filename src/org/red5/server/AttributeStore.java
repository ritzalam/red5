package org.red5.server;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.red5.server.api.IAttributeStore;

public class AttributeStore implements IAttributeStore {

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
	
	synchronized public void setAttributes(org.red5.server.api.IAttributeStore values) {
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

		
}
