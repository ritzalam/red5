package org.red5.server.net;

import org.apache.mina.common.ByteBuffer;

public interface SimpleProtocolEncoder {

	
	public ByteBuffer encode(ProtocolState state, Object out) throws Exception;
	
}
