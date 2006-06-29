package org.red5.server.service;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006 by respective authors (see below). All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either version 2.1 of the License, or (at your option) any later 
 * version. 
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this library; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 */

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
