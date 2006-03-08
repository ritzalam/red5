package org.red5.server.net.remoting.codec;

import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.red5.io.object.Deserializer;
import org.red5.io.object.Serializer;

public class RemotingCodecFactory implements ProtocolCodecFactory {
	
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

	public ProtocolDecoder getDecoder() {
		return decoder;
	}

	public ProtocolEncoder getEncoder() {
		return encoder;
	}

}
