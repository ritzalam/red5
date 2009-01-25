package org.red5.server.stream;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2008 by respective authors (see below). All rights reserved.
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

import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.net.rtmp.message.Constants;

public class StreamTracker implements Constants {
	
    /**
     * Last audio flag
     */
	private int lastAudio;
    /**
     * Last video flag
     */
	private int lastVideo;
    /**
     * Last notification flag
     */
	private int lastNotify;
    /**
     * Relative flag
     */
	private boolean relative;
    /**
     * First video flag
     */
	private boolean firstVideo;
    /**
     * First audio flag
     */
	private boolean firstAudio;
    /**
     * First notification flag
     */
	private boolean firstNotify;

	/** Constructs a new StreamTracker. */
    public StreamTracker() {
		reset();
	}

    /**
     * Reset state
     */
    public void reset() {
		lastAudio = 0;
		lastVideo = 0;
		lastNotify = 0;
		firstVideo = true;
		firstAudio = true;
		firstNotify = true;
	}

    /**
     * RTMP event handler
     * @param event      RTMP event
     * @return           Timeframe since last notification (or auido or video packet sending)
     */
    public int add(IRTMPEvent event) {
		relative = true;
		int timestamp = event.getTimestamp();
		int tsOut = 0;

		switch (event.getDataType()) {

			case TYPE_AUDIO_DATA:
				if (firstAudio) {
					tsOut = event.getTimestamp();
					relative = false;
					firstAudio = false;
				} else {
					tsOut = timestamp - lastAudio;
				}
				lastAudio = timestamp;
				break;

			case TYPE_VIDEO_DATA:
				if (firstVideo) {
					tsOut = event.getTimestamp();
					relative = false;
					firstVideo = false;
				} else {
					tsOut = timestamp - lastVideo;
				}
				lastVideo = timestamp;
				break;

			case TYPE_NOTIFY:
			case TYPE_FLEX_STREAM_SEND:
				// Fix for APPSERVER-329
				// The timer should be set to absolute for
				// org.red5.server.stream.consumer.ConnectionConsumer line 122:
				// header.setTimerRelative(streamTracker.isRelative());
				// TYPE_FLEX_STREAM_SEND is allowed to fall through to TYPE_INVOKE 
			case TYPE_INVOKE:
				if (firstNotify) {
					tsOut = event.getTimestamp();
					relative = false;
					firstNotify = false;
				} else {
					tsOut = timestamp - lastNotify;
				}
				lastNotify = timestamp;
				break;

			default:
				// ignore other types
				break;

		}
		return tsOut;
	}

	/**
     * Getter for property 'relative'.
     *
     * @return Value for property 'relative'.
     */
    public boolean isRelative() {
		return relative;
	}
}