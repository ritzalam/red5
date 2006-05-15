package org.red5.server.stream;

import org.red5.server.api.stream.IBroadcastStreamNew;
import org.red5.server.api.stream.IStream;
import org.red5.server.api.stream.ISubscriberStreamNew;
import org.red5.server.messaging.IMessageOutput;
import org.red5.server.messaging.IPipe;
import org.red5.server.messaging.InMemoryPushPushPipe;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.stream.consumer.ConnectionConsumer;

public class ConsumerService implements IConsumerService {

	public IMessageOutput getConsumerOutput(IStream stream) {
		RTMPConnection conn = null;
		if (stream instanceof ISubscriberStreamNew) {
			ISubscriberStreamNew ss = (ISubscriberStreamNew) stream;
			conn = (RTMPConnection) ss.getConnection();
		} else if (stream instanceof IBroadcastStreamNew) {
			IBroadcastStreamNew bs = (IBroadcastStreamNew) stream;
			conn = (RTMPConnection) bs.getConnection();
		}
		if (conn == null) return null;
		OutputStream o = conn.createOutputStream(stream.getStreamId());
		IPipe pipe = new InMemoryPushPushPipe();
		pipe.subscribe(new ConnectionConsumer(
				conn,
				o.getVideo().getId(),
				o.getAudio().getId(),
				o.getData().getId()), null);
		return pipe;
	}

}
