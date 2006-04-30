package org.red5.server.api.stream;

import org.red5.server.api.IScope;

public interface IOnDemandStreamService {

	public final static String ON_DEMAND_STREAM_SERVICE = "onDemandStreamService";
	
	/**
	 * Has the service an on-demand stream with the passed name?
	 *  
	 * @param scope
	 * 		the scope to check for the stream
	 * @param name
	 * 		the name of the stream
	 * @return true if the stream exists, false otherwise
	 */
	public boolean hasOnDemandStream(IScope scope, String name);
	
	/**
	 * Get a stream that can be used for playback of the on-demand stream
	 *  
	 * @param scope
	 * 		the scope to return the stream from
	 * @param name
	 * 		the name of the stream
	 * @return the on-demand stream
	 */
	public IOnDemandStream getOnDemandStream(IScope scope, String name);
	
}
