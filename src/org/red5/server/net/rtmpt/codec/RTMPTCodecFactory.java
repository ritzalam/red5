package org.red5.server.net.rtmpt.codec;

import org.red5.io.object.Deserializer;
import org.red5.io.object.Serializer;
import org.red5.server.net.SimpleProtocolCodecFactory;
import org.red5.server.net.SimpleProtocolDecoder;
import org.red5.server.net.SimpleProtocolEncoder;

public class RTMPTCodecFactory implements SimpleProtocolCodecFactory {

	protected Deserializer deserializer = null;
	protected Serializer serializer = null;
	protected RTMPTProtocolDecoder decoder;
	protected RTMPTProtocolEncoder encoder;
	
	public void init(){
		decoder = new RTMPTProtocolDecoder();
		decoder.setDeserializer(deserializer);
		encoder = new RTMPTProtocolEncoder();
		encoder.setSerializer(serializer);
	}	
	
	public void setDeserializer(Deserializer deserializer) {
		this.deserializer = deserializer;
	}

	public void setSerializer(Serializer serializer) {
		this.serializer = serializer;
	}

	public SimpleProtocolDecoder getSimpleDecoder(){
		return decoder;
	}

	public SimpleProtocolEncoder getSimpleEncoder(){
		return encoder;
	}
}
