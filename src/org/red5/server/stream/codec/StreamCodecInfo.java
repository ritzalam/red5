package org.red5.server.stream.codec;

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

import org.red5.server.api.stream.IStreamCodecInfo;
import org.red5.server.api.stream.IVideoStreamCodec;

public class StreamCodecInfo implements IStreamCodecInfo {

	private boolean audio = false;

	private boolean video = false;

	private IVideoStreamCodec videoCodec = null;

	/** {@inheritDoc} */
    public boolean hasAudio() {
		return audio;
	}

	/**
     * Setter for property 'hasAudio'.
     *
     * @param value Value to set for property 'hasAudio'.
     */
    public void setHasAudio(boolean value) {
		this.audio = value;
	}

	/** {@inheritDoc} */
    public boolean hasVideo() {
		return video;
	}

	/**
     * Setter for property 'hasVideo'.
     *
     * @param value Value to set for property 'hasVideo'.
     */
    public void setHasVideo(boolean value) {
		this.video = value;
	}

	/** {@inheritDoc} */
    public String getAudioCodecName() {
		return null;
	}

	/** {@inheritDoc} */
    public String getVideoCodecName() {
		if (videoCodec == null) {
			return null;
		}

		return videoCodec.getName();
	}

	/** {@inheritDoc} */
    public IVideoStreamCodec getVideoCodec() {
		return videoCodec;
	}

	/**
     * Setter for property 'videoCodec'.
     *
     * @param codec Value to set for property 'videoCodec'.
     */
    public void setVideoCodec(IVideoStreamCodec codec) {
		this.videoCodec = codec;
	}
}
