package org.red5.server.stream;

import org.red5.server.api.IScope;
import org.red5.server.api.stream.IStream;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.net.rtmp.message.Constants;

public abstract class BaseStream implements IStream, Constants {

	protected IScope scope;
	protected int position;
	protected int streamId;
	protected IVideoStreamCodec videoCodec = null;
	
	public BaseStream(IScope scope) {
		this.scope = scope;
		this.position = 0;
		this.streamId = 0;
	}

	public void setStreamId(int streamId) {
		this.streamId = streamId;
	}

	public int getStreamId() {
		return streamId;
	}

	public IScope getScope() {
		return scope;
	}
	
	public boolean canAccept() {
		return true;
	}

	public int getCurrentPosition() {
		return position;
	}

	public void close() {
		this.videoCodec = null;
	}

	public boolean hasVideo() {
		// TODO: implement this
		return true;
	}
	
	public void setVideoCodec(IVideoStreamCodec codec) {
		this.videoCodec = codec;
	}

	public String getVideoCodecName() {
		if (videoCodec == null)
			return null;
		
		return videoCodec.getName();
	}
	
	public boolean hasAudio() {
		// TODO: implement this
		return true;
	}
	
	public String getAudioCodecName() {
		// TODO: implement this
		return null;
	}
	
}
