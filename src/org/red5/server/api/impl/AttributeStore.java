package org.red5.server.api.impl;

import java.util.HashMap;
import java.util.Set;

public class AttributeStore implements org.red5.server.api.AttributeStore {

	private HashMap attributes = new HashMap();
	
	public Set getAttributeNames(){
		return attributes.keySet();
	}

	public Object getAttribute(String name) {
		return attributes.get(name);
	}

	public boolean hasAttribute(String name) {
		return attributes.containsKey(name);
	}
	
	synchronized public void setAttribute(String name, Object value) {
		if(name != null) 	attributes.put(name,value);
	}

	synchronized public void removeAttribute(String name) {
		if(name != null) 	attributes.remove(name);
	}
	
	
	
}
