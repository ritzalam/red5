package org.red5.server.api.service;

import java.util.Set;

public interface IPendingServiceCall extends IServiceCall {

	public abstract Object getResult();
	
	public abstract void setResult(Object result);

	public void registerCallback(IPendingServiceCallback callback);
	
	public void unregisterCallback(IPendingServiceCallback callback);

	public Set<IPendingServiceCallback> getCallbacks();
}
