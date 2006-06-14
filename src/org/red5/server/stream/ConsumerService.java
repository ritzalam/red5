package org.red5.server.stream;

import org.red5.server.api.stream.IClientStream;
import org.red5.server.api.stream.IStreamCapableConnection;
import org.red5.server.messaging.IMessageOutput;
import org.red5.server.messaging.IPipe;
import org.red5.server.messaging.InMemoryPushPushPipe;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.stream.consumer.ConnectionConsumer;

public class ConsumerService implements IConsumerService {

	public IMessageOutput getConsumerOutput(IClientStream stream) {
		IStreamCapableConnection streamConn = stream.getConnection();
		if (!(streamConn instanceof RTMPConnection)) return null;
		RTMPConnection conn = (RTMPConnection) streamConn;
		// TODO Better manage channels.
		// now we use OutputStream as a channel wrapper.
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
