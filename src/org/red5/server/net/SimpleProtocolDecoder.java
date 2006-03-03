package org.red5.server.net;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

public interface SimpleProtocolDecoder {
	
	/**
	 * @param state Stores state for the protocol, ProtocolState is just a marker interface
	 * @param in ByteBuffer of data to be decoded
	 * @return one of three possible values. 
	 * 	null : the object could not be decoded, or some data was skipped, just continue.
	 *     ProtocolState : the decoder was unable to decode the whole object, refer to the protocol state
	 *     Object : something was decoded, continue
	 * @throws ProtocolCodecException
	 */
	public Object decode(ProtocolState state, ByteBuffer in) throws Exception;

}
