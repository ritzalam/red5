package org.red5.server.api.so;

public interface ISharedObjectListener {

	// The following methods will only be called for RTMP connections


	/**
	 * Called when a client connects to a shared object
	 * 
	 * @param so
	 *            the shared object
	 */
	void onSharedObjectConnect(ISharedObject so);

	/**
	 * Called when a shared object attribute is updated
	 * 
	 * @param so
	 *            the shared object
	 * @param key
	 *            the name of the attribute
	 * @param value
	 *            the value of the attribute
	 */
	void onSharedObjectUpdate(ISharedObject so, String key, Object value);

	/**
	 * Called when an attribute is deleted from the shared object
	 * 
	 * @param so
	 *            the shared object
	 * @param key
	 *            the name of the attribute to delete
	 */
	void onSharedObjectDelete(ISharedObject so, String key);

	/**
	 * Called when a shared object method call is sent
	 * 
	 * @param so
	 *            the shared object
	 * @param method
	 *            the method name to call
	 * @param params
	 *            the arguments
	 */
	void onSharedObjectSend(ISharedObject so, String method, Object[] params);

	
}
