package org.red5.server.api;

import java.util.List;

public class ScopeAdapter implements ScopeHandler {

	private boolean canCreateScope = true;
	private boolean canConnect = true;
	private boolean canCallService = true;
	private boolean canBroadcastEvent = true;
	private boolean canConnectSharedObject = true;
	private boolean canDeleteSharedObject = true;
	private boolean canUpdateSharedObject = true;
	private boolean canSendSharedObject = true;
	private boolean canPublishStream = true;
	private boolean canSubscribeStream = true;
	
	public void setCanBroadcastEvent(boolean canBroadcastEvent) {
		this.canBroadcastEvent = canBroadcastEvent;
	}

	public void setCanCallService(boolean canCallService) {
		this.canCallService = canCallService;
	}

	public void setCanConnect(boolean canConnect) {
		this.canConnect = canConnect;
	}

	public void setCanConnectSharedObject(boolean canConnectSharedObject) {
		this.canConnectSharedObject = canConnectSharedObject;
	}

	public void setCanCreateScope(boolean canCreateScope) {
		this.canCreateScope = canCreateScope;
	}

	public void setCanDeleteSharedObject(boolean canDeleteSharedObject) {
		this.canDeleteSharedObject = canDeleteSharedObject;
	}

	public void setCanPublishStream(boolean canPublishStream) {
		this.canPublishStream = canPublishStream;
	}

	public void setCanSendSharedObject(boolean canSendSharedObject) {
		this.canSendSharedObject = canSendSharedObject;
	}

	public void setCanSubscribeStream(boolean canSubscribeStream) {
		this.canSubscribeStream = canSubscribeStream;
	}

	public void setCanUpdateSharedObject(boolean canUpdateSharedObject) {
		this.canUpdateSharedObject = canUpdateSharedObject;
	}

	public boolean canBroadcastEvent(Object event) {
		return canBroadcastEvent;
	}

	public boolean canCallService(Call call) {
		return canCallService;
	}

	public boolean canConnect(Client client, List params) {
		return canConnect;
	}

	public boolean canCreateScope(String contextPath) {
		return canCreateScope;
	}

	public boolean canPublishStream(String stream, String mode) {
		return canPublishStream;
	}

	public boolean canConnectSharedObject(String soName) {
		return canConnectSharedObject;
	}

	public boolean canDeleteSharedObject(SharedObject so, String key) {
		return canDeleteSharedObject;
	}

	public boolean canSendSharedObject(SharedObject so, String method, Object[] params) {
		return canSendSharedObject;
	}

	public boolean canUpdateSharedObject(SharedObject so, String key, Object value) {
		return canUpdateSharedObject;
	}

	public boolean canSubscribeStream(String stream, String mode) {
		return canSubscribeStream;
	}

	public void onConnect(Connection conn) {
		// nothing
	}

	public void onCreateScope(Scope scope) {
		// nothing
	}

	public void onDisconnect(Connection conn) {
		// nothing
	}

	public void onDisposeScope() {
		// nothing
	}

	public void onEventBroadcast(Object event) {
		// nothing
	}

	public void onServiceCall(Call call) {
		// nothing
	}

	public void onSharedObjectConnect(SharedObject so) {
		// nothing
	}

	public void onSharedObjectDelete(SharedObject so, String key) {
		// nothing
	}

	public void onSharedObjectSend(SharedObject so, String method, Object[] params) {
		// nothing
	}

	public void onSharedObjectUpdate(SharedObject so, String key, Object value) {
		// nothing
	}

	public void onStreamPublish(Stream stream) {
		// nothing
	}

	public void onStreamSubscribe(Stream stream) {
		// nothing
	}
	
	public Call postProcessServiceCall(Call call) {
		return call;
	}

	public Call preProcessServiceCall(Call call) {
		return call;
	}
	
}