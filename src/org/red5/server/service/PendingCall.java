package org.red5.server.service;

import java.util.Set;
import java.util.HashSet;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;

public class PendingCall extends Call implements IPendingServiceCall {

	private Object result = null;
	private HashSet<IPendingServiceCallback> callbacks = new HashSet<IPendingServiceCallback>();

    public PendingCall(String method){
    	super(method);
    }
    
    public PendingCall(String method, Object[] args){
    	super(method, args);
    }
    
    public PendingCall(String name, String method, Object[] args){
    	super(name, method, args);
    }
    
	
	/* (non-Javadoc)
	 * @see org.red5.server.service.ServiceCall#getResult()
	 */
	public Object getResult() {
		return result;
	}

	/* (non-Javadoc)
	 * @see org.red5.server.service.temp#setResult(java.lang.Object)
	 */
	public void setResult(Object result) {
		this.result = result;
	}

	public void registerCallback(IPendingServiceCallback callback) {
		callbacks.add(callback);
	}
	
	public void unregisterCallback(IPendingServiceCallback callback) {
		callbacks.remove(callback);
	}

	public Set<IPendingServiceCallback> getCallbacks() {
		return callbacks;
	}
}
