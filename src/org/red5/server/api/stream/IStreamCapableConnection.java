package org.red5.server.api.stream;

public interface IStreamCapableConnection {

	public int reserveStreamId();
	
	public IStream getStreamById(int streamId);
	
	public void deleteStreamById(int streamId);
	
	public IBroadcastStream newBroadcastStream(String name, int streamId);
	
	public ISubscriberStream newSubscriberStream(String name, int streamId);
	
	public IOnDemandStream newOnDemandStream(String name, int streamId);
	
}
