package org.red5.server.stream;

import org.red5.server.net.rtmp.message.Message;

// Very simple class that can be used as temporary stream while waiting for
// the "real" published live stream.
public class TemporaryStream extends Stream implements IStreamSource {

	public TemporaryStream(String name, String mode) {
		super(null, mode);
		this.setName(name);
		this.setSource(this);
		this.setDownstream(new TemporaryDownStream());
	}
	
	public boolean hasMore() {
		return false;
	}
	
	public Message dequeue() {
		return null;
	}
	
	public void close() {}
}
