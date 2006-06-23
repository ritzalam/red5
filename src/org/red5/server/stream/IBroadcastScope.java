package org.red5.server.stream;

import org.red5.server.api.IBasicScope;
import org.red5.server.messaging.IPipe;

public interface IBroadcastScope extends IBasicScope, IPipe {
	public static final String TYPE = "bs";
	public static final String STREAM_ATTRIBUTE = TRANSIENT_PREFIX + "_publishing_stream";
}
