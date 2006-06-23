package org.red5.server.stream.codec;

import org.red5.server.api.stream.IStreamCodecInfo;
import org.red5.server.api.stream.IVideoStreamCodec;

public class StreamCodecInfo implements IStreamCodecInfo {

	private boolean audio = false;
	private boolean video = false;
	private IVideoStreamCodec videoCodec = null;
	
	public boolean hasAudio() {
		return audio;
	}

	public void setHasAudio(boolean value) {
		this.audio = value;
	}
	
	public boolean hasVideo() {
		return video;
	}

	public void setHasVideo(boolean value) {
		this.video = value;
	}
	
	public String getAudioCodecName() {
		return null;
	}

	public String getVideoCodecName() {
		if (videoCodec == null)
			return null;
		
		return videoCodec.getName();
	}

	public IVideoStreamCodec getVideoCodec() {
		return videoCodec;
	}

	public void setVideoCodec(IVideoStreamCodec codec) {
		this.videoCodec = codec;
	}
}
