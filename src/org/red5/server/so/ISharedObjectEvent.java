package org.red5.server.so;

/**
 * One update event for a shared object received through a connection.
 */
public interface ISharedObjectEvent {

	enum Type {
		SERVER_CONNECT,
		SERVER_DISCONNECT,
		SERVER_SET_ATTRIBUTE,
		SERVER_DELETE_ATTRIBUTE,
		SERVER_SEND_MESSAGE,
		CLIENT_CLEAR_DATA,
		CLIENT_DELETE_ATTRIBUTE,
		CLIENT_DELETE_DATA,
		CLIENT_INITIAL_DATA,
		CLIENT_STATUS,
		CLIENT_UPDATE_DATA,
		CLIENT_UPDATE_ATTRIBUTE,
		CLIENT_SEND_MESSAGE
	};
	
	/**
	 * Returns the type of the event.
	 * 
	 * @return the type of the event.
	 */
	public Type getType();
	
	/**
	 * Returns the key of the event.
	 * 
	 * Depending on the type this contains:
	 * <ul>
	 * <li>the attribute name to set for SET_ATTRIBUTE</li>
	 * <li>the attribute name to delete for DELETE_ATTRIBUTE</li>
	 * <li>the handler name to call for SEND_MESSAGE</li>
	 * </ul>
	 * In all other cases the key is <code>null</code>.
	 * 
	 * @return the key of the event
	 */
	public String getKey();
	
	/**
	 * Returns the value of the event.
	 * 
	 * Depending on the type this contains:
	 * <ul>
	 * <li>the attribute value to set for SET_ATTRIBUTE</li>
	 * <li>a list of parameters to pass to the handler for SEND_MESSAGE</li>
	 * </ul>
	 * In all other cases the value is <code>null</code>.
	 * 
	 * @return the value of the event
	 */
	public Object getValue();
}
