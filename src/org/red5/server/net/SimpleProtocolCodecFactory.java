package org.red5.server.net;

public interface SimpleProtocolCodecFactory {

	public SimpleProtocolDecoder getSimpleDecoder();
	public SimpleProtocolEncoder getSimpleEncoder();
	
}
