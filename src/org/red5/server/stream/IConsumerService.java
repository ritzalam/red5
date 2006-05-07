package org.red5.server.stream;

import org.red5.server.api.stream.IStream;
import org.red5.server.messaging.IConsumer;

public interface IConsumerService {
	public static final String KEY = "consumerService";
	
	IConsumer getConsumer(IStream stream);
}
