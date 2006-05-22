package org.red5.server.api.persistence;

public interface IPersistable {
	
	public boolean isPersistent();
	public String getName();
	public String getType();
	public String getPath();
	public long getLastModified();
	public Object convertToSerialzable();
	public boolean loadFromSerializable(Object from);

}
