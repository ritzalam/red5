package org.red5.server.api.stream;

public interface IOnDemandStreamService {

	/**
	 * Has the service an on-demand stream with the passed name?
	 *  
	 * @param name
	 * 		the name of the stream
	 * @return true if the stream exists, false otherwise
	 */
	public boolean hasOnDemandStream(String name);
	
	/**
	 * Get a stream that can be used for playback of the on-demand stream
	 *  
	 * @param name
	 * 		the name of the stream
	 * @return the on-demand stream
	 */
	public IOnDemandStream getOnDemandStream(String name);
	
}
