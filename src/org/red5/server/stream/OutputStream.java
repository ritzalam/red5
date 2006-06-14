package org.red5.server.stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.net.rtmp.Channel;

public class OutputStream {

	protected static Log log =
        LogFactory.getLog(OutputStream.class.getName());
	
	private Channel video;
	private Channel audio;
	private Channel data;
	
	public OutputStream(Channel video, Channel audio, Channel data){
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
