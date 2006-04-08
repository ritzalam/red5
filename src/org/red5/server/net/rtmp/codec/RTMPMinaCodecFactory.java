package org.red5.server.net.rtmp.codec;

import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.red5.io.object.Deserializer;
import org.red5.io.object.Serializer;
import org.red5.server.net.protocol.SimpleProtocolCodecFactory;
import org.red5.server.net.protocol.SimpleProtocolDecoder;
import org.red5.server.net.protocol.SimpleProtocolEncoder;

public class RTMPMinaCodecFactory implements ProtocolCodecFactory, SimpleProtocolCodecFactory {

	protected Deserializer deserializer = null;
	protected Serializer serializer = null;
	protected RTMPMinaProtocolDecoder decoder;
	protected RTMPMinaProtocolEncoder encoder;
	
	public void init(){
		decoder = new RTMPMinaProtocolDecoder();
		decoder.setDeserializer(deserializer);
		encoder = new RTMPMinaProtocolEncoder();
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

	public SimpleProtocolDecoder getSimpleDecoder(){
		return decoder;
	}

	public SimpleProtocolEncoder getSimpleEncoder(){
		return encoder;
	}
	
}
