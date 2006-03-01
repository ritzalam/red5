package org.red5.server.api;

public interface Session {
	
	// This should be as close to the servlet API as possible
	 public Object getAttribute(java.lang.String name);
	 public java.util.Enumeration getAttributeNames();
	 public long getCreationTime();
	 public String 	getId();
	 public  long getLastAccessedTime();
	 public int getMaxInactiveInterval();
	 public void invalidate();
	 public boolean isNew();
	 public void removeAttribute(java.lang.String name);
	 public void setAttribute(String name,Object value);
	 public void setMaxInactiveInterval(int interval);
	
}