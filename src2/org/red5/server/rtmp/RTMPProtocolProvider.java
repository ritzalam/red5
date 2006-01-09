package org.red5.server.rtmp;

import org.apache.mina.protocol.ProtocolCodecFactory;
import org.apache.mina.protocol.ProtocolDecoder;
import org.apache.mina.protocol.ProtocolEncoder;
import org.apache.mina.protocol.ProtocolHandler;
import org.apache.mina.protocol.ProtocolProvider;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class RTMPProtocolProvider implements 
	ProtocolProvider, ApplicationContextAware, ProtocolCodecFactory {

	private ApplicationContext appCtx = null;
	private String protocolEncoderName = "rtmpProtocolEncoder";
	private String protocolDecoderName = "rtmpProtocolDecoder";
	
	// Protocol handler is usually a singleton.
    private ProtocolHandler handler = null;

    public void setApplicationContext(ApplicationContext appCtx) throws BeansException {
		this.appCtx = appCtx;
	}

	public void setHandler(ProtocolHandler protocolHandler) {
		this.handler = protocolHandler;
	}

	public ProtocolEncoder newEncoder(){
        return (ProtocolEncoder) appCtx.getBean(protocolEncoderName);
    }

    public ProtocolDecoder newDecoder(){
        return (ProtocolDecoder) appCtx.getBean(protocolDecoderName);
    }

    public ProtocolCodecFactory getCodecFactory(){
        return this;
    }

    public ProtocolHandler getHandler(){
        return handler;
    }

}
