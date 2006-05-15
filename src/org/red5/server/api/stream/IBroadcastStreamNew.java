package org.red5.server.api.stream;

import org.red5.server.api.IConnection;

public interface IBroadcastStreamNew extends IStream {
	/**
	 * Save the broadcast stream as a file.
	 * @param name
	 * @param isAppend TODO
	 */
	void saveAs(String name, boolean isAppend);
	
	IConnection getConnection();
}
