package org.red5.server.stream;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006 by respective authors (see below). All rights reserved.
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

import org.red5.server.api.IFlowControllable;

/**
 * A service that controls bandwidth of IFlowControllable objects based
 * on token buckets.
 * <p>
 * 1. Each FC is recommended to release itself by calling
 * releaseFlowControllable on disposal.
 * </p>
 * <p>
 * 2. When parent FC has been released, child FC should be released or should
 * not use any functional method like getAudioTokenBucket etc.
 * </p>
 * <p>
 * TODO we should provide a cleaner cleanup with IEvent/IEventListener
 * </p>
 * @author The Red5 Project (red5@osflash.org)
 * @author Steven Gong (steven.gong@gmail.com)
 */
public interface IFlowControlService {
	public static final String KEY = "FlowControlService";

	/**
	 * Release a flow controllable and related resources when necessary.
	 * 
	 * @param fc             Flow controllable
	 */
	void releaseFlowControllable(IFlowControllable fc);

	/**
	 * Update configuration of buckets according to BW configuration
	 * of the flow controllable.
	 * @param fc             Flow controllable
	 */
	void updateBWConfigure(IFlowControllable fc);

	/**
	 * Reset all token buckets that are assigned to the
	 * flow controllable.
	 * @param fc             Flow controllable
	 */
	void resetTokenBuckets(IFlowControllable fc);

	/**
	 * Get the audio bucket for a flow controllable.
	 * The bucket can be used till releasing regardless of
	 * BW configuration changes.
	 * @param fc             Flow controllable
	 * @return               Audio bucket
	 */
	ITokenBucket getAudioTokenBucket(IFlowControllable fc);

	/**
	 * Get the video bucket for a flow controllable.
	 * The bucket can be used till releasing regardless of
	 * BW configuration changes.
	 * @param fc             Flow controllable
	 * @return               Video bucket
	 */
	ITokenBucket getVideoTokenBucket(IFlowControllable fc);
}
