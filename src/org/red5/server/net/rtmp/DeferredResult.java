package org.red5.server.net.rtmp;

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

import java.lang.ref.WeakReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.net.rtmp.event.Invoke;

/**
 * Can be returned to delay returning the result of invoked methods.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class DeferredResult {

	protected static Log log = LogFactory.getLog(DeferredResult.class.getName());
	
	private WeakReference<Channel> channel;
	private IPendingServiceCall call;
	private int invokeId;
	private boolean resultSent = false;
	
	/**
	 * Set the result of a method call and send to the caller.
	 * 
	 * @param result
	 * 			deferred result of the method call
	 */
	public void setResult(Object result) {
		if (this.resultSent)
			throw new RuntimeException("You can only set the result once.");

		this.resultSent = true;
		Channel channel = this.channel.get();
		if (channel == null) {
			log.warn("The client is no longer connected.");
			return;
		}
		
		Invoke reply = new Invoke();
		call.setResult(result);
		reply.setCall(call);
		reply.setInvokeId(invokeId);
		channel.write(reply);
		channel.getConnection().unregisterDeferredResult(this);
	}

	/**
	 * Check if the result has been sent to the client.
	 * 
	 * @return <code>true</code> if the result has been sent, otherwise <code>false</code> 
	 */
	public boolean wasSent() {
		return resultSent;
	}
	
	/**
     * Setter for property 'invokeId'.
     *
     * @param id Value to set for property 'invokeId'.
     */
    protected void setInvokeId(int id) {
		this.invokeId = id;
	}
	
	/**
     * Setter for property 'serviceCall'.
     *
     * @param call Value to set for property 'serviceCall'.
     */
    protected void setServiceCall(IPendingServiceCall call) {
		this.call = call;
	}
	
	/**
     * Setter for property 'channel'.
     *
     * @param channel Value to set for property 'channel'.
     */
    protected void setChannel(Channel channel) {
		this.channel = new WeakReference<Channel>(channel);
	}
}
