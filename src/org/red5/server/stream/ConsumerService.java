package org.red5.server.stream;

import org.red5.server.api.stream.ISubscriberStreamNew;
import org.red5.server.api.stream.IStream;
import org.red5.server.messaging.IConsumer;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.stream.consumer.ConnectionConsumer;

public class ConsumerService implements IConsumerService {

	public IConsumer getConsumer(IStream stream) {
		if (!(stream instanceof ISubscriberStreamNew)) return null;
		ISubscriberStreamNew ss = (ISubscriberStreamNew) stream;
		RTMPConnection conn = (RTMPConnection) ss.getConnection();
		OutputStream o = conn.createOutputStream(ss.getStreamId());
		return new ConnectionConsumer(
				conn,
				o.getVideo().getId(),
				o.getAudio().getId(),
				o.getData().getId());
	}

}
