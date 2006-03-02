package org.red5.server.api;

import java.util.List;

public interface ScopeHandler {
	
	boolean canCreateScope(String contextPath);
	void onCreateScope(Scope scope);
	void onDisposeScope();
	
	boolean canConnect(Client client, List params); // ? list or object[] ?
	void onConnect(Connection conn);
	void onDisconnect(Connection conn);
	
	boolean canCallService(Call call);
	Call preProcessServiceCall(Call call);
	void onServiceCall(Call call);
	Call postProcessServiceCall(Call call);
	
	boolean canBroadcastEvent(Object event);
	void onEventBroadcast(Object event);
	
	// The following methods will only be called for RTMP connections
	
	boolean canPublishStream(String stream, String mode); 
	void onStreamPublish(Stream stream);
	
	boolean canSubscribeStream(String stream, String mode);
	void onStreamSubscribe(Stream stream);
	
	boolean canConnectSharedObject(String soName);
	void onSharedObjectConnect(SharedObject so);
	
	boolean canUpdateSharedObject(SharedObject so, String key, Object value); 
	void onSharedObjectUpdate(SharedObject so, String key, Object value);
	
	boolean canDeleteSharedObject(SharedObject so, String key);
	void onSharedObjectDelete(SharedObject so, String key);
	
	boolean canSendSharedObject(SharedObject so, String method, Object[] params);
	void onSharedObjectSend(SharedObject so, String method, Object[] params);

}