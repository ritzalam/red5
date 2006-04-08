package org.red5.server.api;

/**
 * Provides a registry of client objects.
 * You can lookup a client by its clientid / session id.
 * 
 * @author luke
 */
public interface IClientRegistry {

	public boolean hasClient(String id);
	public IClient newClient();
	public IClient lookupClient(String id);

}