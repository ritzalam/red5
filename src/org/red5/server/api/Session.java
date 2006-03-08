package org.red5.server.api;

import java.util.Set;

public interface Session {
	
	public long getCreationTime();
	public boolean isNew();
	 
	public String getId();
	public void invalidate();

	public Set getAttributeNames();
	public void setAttribute(String name,Object value);
	public Object getAttribute(String name);
	public boolean hasAttribute(String name);
	public void removeAttribute(String name);

}