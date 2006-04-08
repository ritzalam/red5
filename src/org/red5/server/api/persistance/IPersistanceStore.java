package org.red5.server.api.persistance;

public interface IPersistanceStore {
	
	public boolean save(IPersistable obj);
	public boolean load(IPersistable obj);
	
}
