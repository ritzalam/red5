package org.red5.server.stream;

import org.red5.server.api.stream.IStream;
import org.red5.server.messaging.IMessageOutput;

public interface IConsumerService {
	public static final String KEY = "consumerService";
	
	IMessageOutput getConsumerOutput(IStream stream);
}
