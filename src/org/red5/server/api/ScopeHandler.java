package org.red5.server.api;

public interface ScopeHandler {
	
	boolean canCreateScope(String contextPath);
	void onCreateScope(Scope scope);
	void onDisposeScope();
	
	boolean canConnect(Connection conn);
	void onConnect(Connection conn);
	void onDisconnect(Connection conn);
	
	boolean canCallService(Call call);
	Call preProcessServiceCall(Call call);
	void onServiceCall(Call call);
	Call postProcessServiceCall(Call call);
	
	boolean canBroadcastEvent(Object event);
	void onEventBroadcast(Object event);
	
	// The following methods will only be called for RTMP connections
	
	boolean canPublishStream(String name); 
	void onStreamPublishStart(Stream stream);
	void onStreamPublishStop(Stream stream);
	
	boolean canBroadcastStream(String name);
	void onBroadcastStreamStart(Stream stream);
	
	boolean canRecordStream(String name);
	void onRecordStreamStart(Stream stream);
	void onRecordStreamStop(Stream stream);
	
	boolean canSubscribeToBroadcastStream(String name);
	void onBroadcastStreamSubscribe(BroadcastStream stream);
	void onBroadcastStreamUnsubscribe(BroadcastStream stream);
	
	boolean canConnectToOnDemandStream(String name);
	void onOnDemandStreamConnect(OnDemandStream stream);
	void onOnDemandStreamDisconnect(OnDemandStream stream);
	
	boolean canConnectSharedObject(String soName);
	void onSharedObjectConnect(SharedObject so);
	
	boolean canUpdateSharedObject(SharedObject so, String key, Object value); 
	void onSharedObjectUpdate(SharedObject so, String key, Object value);
	
	boolean canDeleteSharedObject(SharedObject so, String key);
	void onSharedObjectDelete(SharedObject so, String key);
	
	boolean canSendSharedObject(SharedObject so, String method, Object[] params);
	void onSharedObjectSend(SharedObject so, String method, Object[] params);

}