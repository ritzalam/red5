package org.red5.server.api;

import java.util.Set;

public interface AttributeStore {
	
	public Set getAttributeNames();
	public void setAttribute(String name,Object value);
	public Object getAttribute(String name);
	public boolean hasAttribute(String name);
	public void removeAttribute(String name);
	
}
