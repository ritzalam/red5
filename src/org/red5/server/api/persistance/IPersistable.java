package org.red5.server.api.persistance;

public interface IPersistable {
	
	public boolean isPersistant();
	public String getName();
	public String getType();
	public String getPath();
	public long getLastModified();
	public Object convertToSerialzable();
	public boolean loadFromSerializable(Object from);

}
