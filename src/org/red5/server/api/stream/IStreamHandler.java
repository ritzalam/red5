package org.red5.server.api.stream;

public interface IStreamHandler {
	
	/**
	 * Called when the client begins publishing
	 * 
	 * @param stream
	 *            the stream object
	 */
	void onStreamPublishStart(IStream stream);

	/**
	 * Called when the client stops publishing
	 * 
	 * @param stream
	 *            the stream object
	 */
	void onStreamPublishStop(IStream stream);

	/**
	 * Called when the broadcast starts
	 * 
	 * @param stream
	 *            the stream object
	 */
	void onBroadcastStreamStart(IStream stream);

	/**
	 * Called when a recording starts
	 * 
	 * @param stream
	 *            the stream object
	 */
	void onRecordStreamStart(IStream stream);

	/**
	 * Called when a recording stops
	 * 
	 * @param stream
	 *            the stream object
	 */
	void onRecordStreamStop(IStream stream);

	/**
	 * Called when a client subscribes to a broadcast
	 * 
	 * @param stream
	 *            the stream object
	 */
	void onBroadcastStreamSubscribe(IBroadcastStream stream);

	/**
	 * Called when a client unsubscribes from a broadcast
	 * 
	 * @param stream
	 *            the stream object
	 */
	void onBroadcastStreamUnsubscribe(IBroadcastStream stream);

	/**
	 * Called when a client connects to an on demand stream
	 * 
	 * @param stream
	 *            the stream object
	 */
	void onOnDemandStreamConnect(IOnDemandStream stream);

	/**
	 * Called when a client disconnects from an on demand stream
	 * 
	 * @param stream
	 *            the stream object
	 */
	void onOnDemandStreamDisconnect(IOnDemandStream stream);

}
