package org.red5.server.api.persistence;

public interface IPersistenceStore {
	
	public boolean save(IPersistable obj);
	public boolean load(IPersistable obj);
	
}
