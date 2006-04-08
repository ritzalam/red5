package org.red5.server.api.so;

import java.util.Iterator;

public interface ISharedObjectCapableConnection {
	
	public boolean isConnectedToSharedObject(String name);
	public Iterator<String> getConnectedSharedObjectNames();
	public boolean connectSharedObject(String name, boolean persistent);
	public void disconnectSharedObject(String name);
	public ISharedObject getConnectedSharedObject(String name);

}