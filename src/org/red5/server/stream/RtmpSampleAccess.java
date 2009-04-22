package org.red5.server.stream;

import org.red5.server.api.IScope;
import org.red5.server.api.stream.IRtmpSampleAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default RtmpSampleAccess bean
 * @see org.red5.server.api.stream.IRtmpSampleAccess
 */
public class RtmpSampleAccess implements IRtmpSampleAccess {

	private static Logger logger = LoggerFactory.getLogger(RtmpSampleAccess.class);
	
	private boolean audioAllowed = false;
	private boolean videoAllowed = false;

	/**
	 * Setter audioAllowed
	 * @param permission
	 */
	public void setAudioAllowed(boolean permission) {
		logger.debug("setAudioAllowed = "+permission);
		audioAllowed = permission;
	}
	
	/**
	 * Setter videoAllowed
	 * @param permission
	 */
	public void setVideoAllowed(boolean permission) {
		logger.debug("setVideoAllowed = "+permission);
		videoAllowed = permission;
	}
	
	/** {@inheritDoc} */
	public boolean isAudioAllowed(IScope scope) {
		logger.debug("isAudioAllowed = "+audioAllowed);
		return audioAllowed;
	}

	/** {@inheritDoc} */
	public boolean isVideoAllowed(IScope scope) {
		logger.debug("isVideoAllowed = "+videoAllowed);
		return videoAllowed;
	}

}
