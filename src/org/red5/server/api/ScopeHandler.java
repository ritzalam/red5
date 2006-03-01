package org.red5.server.api;

import java.util.List;

public interface ScopeHandler {

	boolean onClientConnect(List params); // ? list or object[] ?
	void onClientDisconnect();
	
	boolean onServiceCall(String service, String method, Object[] params);
	boolean onEventBroadcast(Object event);
	
	// The following methods will only be called for RTMP connections
	
	boolean onStreamPublish(String stream, String mode); 
	boolean onStreamSubscribe(String stream, String mode);

	boolean onSharedObjectConnect(String so);
	boolean onSharedObjectUpdate(String so, String key, Object value); 
	boolean onSharedObjectDelete(String so, String key);
	boolean onSharedObjectSend(String so, String method, Object[] params);
		
}
