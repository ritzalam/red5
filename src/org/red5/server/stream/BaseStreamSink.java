package org.red5.server.stream;

import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.Message;

public class BaseStreamSink implements IStreamSink, Constants {

	protected ISinkContainer sinkContainer = null;
	protected IVideoStreamCodec videoCodec = null;
	
	public boolean canAccept() {
		return true;
	}

	public void enqueue(Message message) {
		// override this...
	}

	public void close() {
		if (this.sinkContainer != null)
			this.sinkContainer.disconnect(this);
		
		this.videoCodec = null;
	}

	public void setVideoCodec(IVideoStreamCodec codec) {
		this.videoCodec = codec;
	}

	public void setSinkContainer(ISinkContainer container) {
		this.sinkContainer = container;
	}

}
