package org.red5.server.stream;

import org.red5.server.api.stream.IClientStream;
import org.red5.server.messaging.IMessageOutput;

public interface IConsumerService {
	public static final String KEY = "consumerService";
	
	IMessageOutput getConsumerOutput(IClientStream stream);
}
