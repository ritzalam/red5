package org.red5.server.net.protocol;

public interface SimpleProtocolCodecFactory {

	public SimpleProtocolDecoder getSimpleDecoder();
	public SimpleProtocolEncoder getSimpleEncoder();
	
}
