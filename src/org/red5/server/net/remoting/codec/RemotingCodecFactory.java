package org.red5.server.net.remoting.codec;

import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.red5.io.object.Deserializer;
import org.red5.io.object.Serializer;
import org.red5.server.net.protocol.SimpleProtocolCodecFactory;
import org.red5.server.net.protocol.SimpleProtocolDecoder;
import org.red5.server.net.protocol.SimpleProtocolEncoder;

public class RemotingCodecFactory implements SimpleProtocolCodecFactory {
	
	protected Deserializer deserializer;
	protected Serializer serializer;
	protected RemotingProtocolDecoder decoder;
	protected RemotingProtocolEncoder encoder;
	
	public void init(){
		decoder = new RemotingProtocolDecoder();
		decoder.setDeserializer(deserializer);
		encoder = new RemotingProtocolEncoder();
		encoder.setSerializer(serializer);
	}
	
	public void setDeserializer(Deserializer deserializer) {
		this.deserializer = deserializer;
	}

	public void setSerializer(Serializer serializer) {
		this.serializer = serializer;
	}

	public SimpleProtocolDecoder getSimpleDecoder() {
		return decoder;
	}

	public SimpleProtocolEncoder getSimpleEncoder() {
		return encoder;
	}	
	
}
