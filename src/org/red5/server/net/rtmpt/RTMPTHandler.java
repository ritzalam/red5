package org.red5.server.net.rtmpt;

import org.red5.server.net.SimpleProtocolCodecFactory;
import org.red5.server.net.rtmp.RTMPHandler;
import org.red5.server.net.rtmp.message.Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.xml.XmlConfiguration;

public class RTMPTHandler extends RTMPHandler implements Constants {

	protected static Log log =
        LogFactory.getLog(RTMPTHandler.class.getName());
	
	public static final String HANDLER_ATTRIBUTE = "red5.RMPTHandler";
	
	public XmlConfiguration rtmptServerConfiguration = null;
	
	protected Server rtmptServer = null;
	protected SimpleProtocolCodecFactory codecFactory = null;
	
	public void setCodecFactory(SimpleProtocolCodecFactory factory) {
		this.codecFactory = factory;		
	}
	
	public SimpleProtocolCodecFactory getCodecFactory() {
		return this.codecFactory;		
	}
	
	public void setRtmptServerConfiguration(XmlConfiguration config) throws Exception {
		rtmptServerConfiguration = config;
		rtmptServer = (Server) config.newInstance();
		
		// Setup configuration data in rtmptServer
		Handler tmp = rtmptServer.getHandler();
		if (!(tmp instanceof ContextHandler))
			throw new Exception("Only context handlers supported.");
		
		((ContextHandler) tmp).setAttribute(HANDLER_ATTRIBUTE, this);
	}
	
}
