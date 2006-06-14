package org.red5.server.api.stream;

import org.red5.server.api.IConnection;

/**
 * A connection that supports streaming.
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard (luke@codegent.com)
 * @author Steven Gong (steven.gong@gmail.com)
 */
public interface IStreamCapableConnection extends IConnection {

	/**
	 * Return a reserved stream id for use.
	 * According to FCS/FMS regulation, the base is 1.
	 * @return
	 */
	int reserveStreamId();
	
	/**
	 * Unreserve this id for future use.
	 * @param streamId
	 */
	void unreserveStreamId(int streamId);
	
	/**
	 * Get a stream by its id.
	 * @param streamId
	 * @return
	 */
	IClientStream getStreamById(int streamId);

	/**
	 * Create a stream that can play only one item.
	 * @param streamId
	 * @return
	 */
	ISingleItemSubscriberStream newSingleItemSubscriberStream(int streamId);
	
	/**
	 * Create a stream that can play a list.
	 * @param streamId
	 * @return
	 */
	IPlaylistSubscriberStream newPlaylistSubscriberStream(int streamId);

	/**
	 * Create a broadcast stream.
	 * @param streamId
	 * @return
	 */
	IClientBroadcastStream newBroadcastStream(int streamId);

}