package org.red5.server.stream;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2009 by respective authors (see below). All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either version 2.1 of the License, or (at your option) any later 
 * version. 
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this library; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 */

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
