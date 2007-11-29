package org.red5.server.net.mrtmp.codec;

import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

public class MRTMPCodecFactory implements ProtocolCodecFactory {
	
	private ProtocolDecoder decoder = new MRTMPProtocolDecoder();
	private ProtocolEncoder encoder = new MRTMPProtocolEncoder();

	public ProtocolDecoder getDecoder() throws Exception {
		return decoder;
	}

	public ProtocolEncoder getEncoder() throws Exception {
		return encoder;
	}

}
