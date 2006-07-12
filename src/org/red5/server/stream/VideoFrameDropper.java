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

import static org.red5.server.net.rtmp.event.VideoData.FrameType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.net.rtmp.event.VideoData;

/**
 * State machine for video frame dropping in live streams.
 * 
 * We start sending all frame types. Disposable interframes can be dropped any
 * time without affecting the current state. If a regular interframe is dropped,
 * all future frames up to the next keyframes are dropped as well. Dropped
 * keyframes result in only keyframes being sent. If two consecutive keyframes
 * have been successfully sent, regular interframes will be sent in the next
 * iteration as well. If these frames all went through, disposable interframes
 * are sent again.
 * 
 * So from highest to lowest bandwidth and back, the states go as follows:
 * - all frames
 * - keyframes and interframes
 * - keyframes
 * - keyframes and interframes
 * - all frames
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class VideoFrameDropper implements IFrameDropper {

	protected static Log log =
        LogFactory.getLog(VideoFrameDropper.class.getName());

	/** Current state. */
	private int state;
	/** Timestamps of the dropped packets. */
	private int droppedTimes;
	/** Should the timestamps of dropped packets be counted? */
	private boolean countDroppedTimes;
	
	public VideoFrameDropper() {
		reset();
	}
	
	public void reset() {
		reset(SEND_ALL);
	}
	
	public void reset(int state) {
		this.state = state;
		droppedTimes = 0;
		countDroppedTimes = (state != SEND_ALL);
	}
	
	public boolean canSendPacket(IRTMPEvent packet, long pending) {
		if (! (packet instanceof VideoData))
			// We currently only drop video packets.
			return true;
		
		VideoData video = (VideoData) packet;
		FrameType type = video.getFrameType();
		boolean result = false;
		switch (state) {
		case SEND_ALL:
			// All packets will be sent.
			result = true;
			break;
			
		case SEND_INTERFRAMES:
			// Only keyframes and interframes will be sent.
			if (type == FrameType.KEYFRAME) {
				if (pending == 0) {
					// Send all frames from now on.
					state = SEND_ALL;
					countDroppedTimes = true;
				}
				result = true;
			} else if (type == FrameType.INTERFRAME)
				result = true;
			break;

		case SEND_KEYFRAMES:
			// Only keyframes will be sent.
			result = (type == FrameType.KEYFRAME);
			if (result && pending == 0)
				// Maybe switch back to SEND_INTERFRAMES after the next keyframe
				state = SEND_KEYFRAMES_CHECK;
			break;
			
		case SEND_KEYFRAMES_CHECK:
			// Only keyframes will be sent.
			result = (type == FrameType.KEYFRAME);
			if (result && pending == 0)
				// Continue with sending interframes as well
				state = SEND_INTERFRAMES;
			break;
		}
		
		// Store timestamp of dropped packet
		if (!result && countDroppedTimes && type != FrameType.DISPOSABLE_INTERFRAME)
			droppedTimes += packet.getTimestamp();
		
		return result;
	}

	public void dropPacket(IRTMPEvent packet) {
		if (! (packet instanceof VideoData))
			// Only check video packets.
			return;
		
		VideoData video = (VideoData) packet;
		FrameType type = video.getFrameType();
		
		// Store timestamp of dropped packet
		if (countDroppedTimes && type != FrameType.DISPOSABLE_INTERFRAME)
			droppedTimes += packet.getTimestamp();
		
		switch (state) {
		case SEND_ALL:
			if (type == FrameType.DISPOSABLE_INTERFRAME) {
				// Remain in state, packet is safe to drop.
				return;
			} else if (type == FrameType.INTERFRAME) {
				// Drop all frames until the next keyframe.
				state = SEND_KEYFRAMES;
				return;
			} else if (type == FrameType.KEYFRAME) {
				// Drop all frames until the next keyframe.
				state = SEND_KEYFRAMES;
				return;
			}
			break;
			
		case SEND_INTERFRAMES:
			if (type == FrameType.INTERFRAME) {
				// Drop all frames until the next keyframe.
				state = SEND_KEYFRAMES_CHECK;
				return;
			} else if (type == FrameType.KEYFRAME) {
				// Drop all frames until the next keyframe.
				state = SEND_KEYFRAMES;
				return;
			}
			break;
			
		case SEND_KEYFRAMES:
			// Remain in state.
			break;
			
		case SEND_KEYFRAMES_CHECK:
			if (type == FrameType.KEYFRAME) {
				// Switch back to sending keyframes, but don't move to
				// SEND_INTERFRAMES afterwards.
				state = SEND_KEYFRAMES;
				return;
			}
			break;
		}
	}

	public void sendPacket(IRTMPEvent packet) {
		if (! (packet instanceof VideoData))
			// Only process video packets.
			return;
		
		if (droppedTimes == 0)
			return;
		
		// Modify packet to send to include dropped timestamps
		VideoData video = (VideoData) packet;
		log.debug("Dropped " + droppedTimes + " ms packets");
		video.setTimestamp(video.getTimestamp() + droppedTimes);
		droppedTimes = 0;
	}

}
