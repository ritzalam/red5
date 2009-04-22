package org.red5.server.api.stream;

import org.red5.server.api.IScope;

public interface IRtmpSampleAccess {
	
	public static String BEAN_NAME = "rtmpSampleAccess";

	/**
	 * Return true if sample access allowed on audio stream
	 * @param scope
	 * @return
	 */
	public boolean isAudioAllowed(IScope scope);
	
	/**
	 * Return true if sample access allowed on video stream
	 * @param scope
	 * @return
	 */
	public boolean isVideoAllowed(IScope scope);
	
}
