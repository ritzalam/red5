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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.net.rtmp.Channel;

public class OutputStream {

	protected static Log log = LogFactory.getLog(OutputStream.class.getName());

	private Channel video;

	private Channel audio;

	private Channel data;

	public OutputStream(Channel video, Channel audio, Channel data) {
		this.video = video;
		this.audio = audio;
		this.data = data;
	}

	public void close() {
		this.video.close();
		this.audio.close();
		this.data.close();
	}

	public Channel getAudio() {
		return audio;
	}

	public Channel getData() {
		return data;
	}

	public Channel getVideo() {
		return video;
	}
}
