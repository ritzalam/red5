package org.red5.server.net.rtmp;

import org.red5.server.context.AppContext;
import org.red5.server.service.ServiceInvoker;
import org.red5.server.stream.StreamManager;

public class Context {

	private StreamManager streamManager;
	private ServiceInvoker serviceInvoker;
	private AppContext appContext;
	
	public AppContext getAppContext() {
		return appContext;
	}
	
	public ServiceInvoker getServiceInvoker() {
		return serviceInvoker;
	}
	
	public StreamManager getStreamManager() {
		return streamManager;
	}	
	
}
