package org.red5.server.api.impl;

import org.red5.server.api.BroadcastStream;
import org.red5.server.api.Call;
import org.red5.server.api.Connection;
import org.red5.server.api.OnDemandStream;
import org.red5.server.api.Scope;
import org.red5.server.api.ScopeHandler;
import org.red5.server.api.SharedObject;
import org.red5.server.api.Stream;


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
	private boolean canRecordStream = true;
	private boolean canBroadcastStream = true;
	private boolean canSubscribeToBroadcastStream = true;
	private boolean canConnectToOnDemandStream = true;
	
	public void setCanCreateScope(boolean canCreateScope) {
		this.canCreateScope = canCreateScope;
	}

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

	public void setCanDeleteSharedObject(boolean canDeleteSharedObject) {
		this.canDeleteSharedObject = canDeleteSharedObject;
	}

	public void setCanSendSharedObject(boolean canSendSharedObject) {
		this.canSendSharedObject = canSendSharedObject;
	}

	public void setCanUpdateSharedObject(boolean canUpdateSharedObject) {
		this.canUpdateSharedObject = canUpdateSharedObject;
	}

	public void setCanPublishStream(boolean canPublishStream) {
		this.canPublishStream = canPublishStream;
	}
	
	public void setCanRecordStream(boolean canRecordStream) {
		this.canRecordStream = canRecordStream;
	}

	public void setCanBroadcastStream(boolean canBroadcastStream) {
		this.canBroadcastStream = canBroadcastStream;
	}

	public void setCanConnectToOnDemandStream(boolean canConnectToOnDemandStream) {
		this.canConnectToOnDemandStream = canConnectToOnDemandStream;
	}

	public void setCanSubscribeToBroadcastStream(boolean canSubscribeToBroadcastStream) {
		this.canSubscribeToBroadcastStream = canSubscribeToBroadcastStream;
	}

	public boolean canCreateScope(String contextPath) {
		return canCreateScope;
	}

	public boolean canBroadcastEvent(Object event) {
		return canBroadcastEvent;
	}

	public boolean canCallService(Call call) {
		return canCallService;
	}

	public boolean canConnect(Connection conn) {
		return canConnect;
	}

	public boolean canConnectSharedObject(String soName) {
		return canConnectSharedObject;
	}

	public boolean canDeleteSharedObject(SharedObject so, String key) {
		return canDeleteSharedObject;
	}

	public boolean canSendSharedObject(SharedObject so, String method, Object[] args) {
		return canSendSharedObject;
	}

	public boolean canUpdateSharedObject(SharedObject so, String key, Object value) {
		return canUpdateSharedObject;
	}

	public boolean canPublishStream(String name) {
		return canPublishStream;
	}

	public boolean canBroadcastStream(String name) {
		return canBroadcastStream;
	}

	public boolean canRecordStream(String name) {
		return canRecordStream;
	}

	public boolean canConnectToOnDemandStream(String name) {
		return canConnectToOnDemandStream;
	}

	public boolean canSubscribeToBroadcastStream(String name) {
		return canSubscribeToBroadcastStream;
	}

	public void onCreateScope(Scope scope) {
		// nothing
	}

	public void onDisposeScope() {
		// nothing
	}

	public void onConnect(Connection conn) {
		// nothing
	}

	public void onDisconnect(Connection conn) {
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

	public void onBroadcastStreamStart(Stream stream) {
		// TODO Auto-generated method stub
	}

	public void onBroadcastStreamSubscribe(BroadcastStream stream) {
		// TODO Auto-generated method stub
	}

	public void onBroadcastStreamUnsubscribe(BroadcastStream stream) {
		// TODO Auto-generated method stub		
	}

	public void onOnDemandStreamConnect(OnDemandStream stream) {
		// TODO Auto-generated method stub
	}

	public void onOnDemandStreamDisconnect(OnDemandStream stream) {
		// TODO Auto-generated method stub
	}

	public void onRecordStreamStart(Stream stream) {
		// TODO Auto-generated method stub
	}

	public void onRecordStreamStop(Stream stream) {
		// TODO Auto-generated method stub
	}

	public void onStreamPublishStart(Stream stream) {
		// TODO Auto-generated method stub
	}

	public void onStreamPublishStop(Stream stream) {
		// TODO Auto-generated method stub
	}

	public Call postProcessServiceCall(Call call) {
		return call;
	}

	public Call preProcessServiceCall(Call call) {
		return call;
	}
	
}