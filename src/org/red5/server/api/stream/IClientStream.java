package org.red5.server.api.stream;

import org.red5.server.api.IFlowControllable;

/**
 * A stream that is bound to a client.
 * @author The Red5 Project (red5@osflash.org)
 * @author Steven Gong (steven.gong@gmail.com)
 */
public interface IClientStream extends IStream, IFlowControllable {
	public static final String MODE_READ = "read";
	public static final String MODE_RECORD = "record";
	public static final String MODE_APPEND = "append";
	public static final String MODE_LIVE = "live";
	
	/**
	 * Get stream id allocated in a connection.
	 * @return
	 */
	int getStreamId();
	
	/**
	 * Get connection containing the stream.
	 * @return
	 */
	IStreamCapableConnection getConnection();
}
