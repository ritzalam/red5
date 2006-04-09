package org.red5.server.stream;

import org.red5.server.api.IScope;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.ISubscriberStream;
import org.red5.server.net.rtmp.RTMPConnection;

public class SubscriberStream extends Stream implements ISubscriberStream {

	private IBroadcastStream broadcast = null;
	
	public SubscriberStream(IScope scope, RTMPConnection conn) {
		super(scope, conn, Stream.MODE_LIVE);
	}

	protected void setBroadcastStream(IBroadcastStream stream) {
		this.broadcast = stream;
	}
	
	public void close() {
		if (this.broadcast != null)
			this.broadcast.unsubscribe(this);
		
		super.close();
	}
	
}
