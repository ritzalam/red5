package org.red5.server.net.remoting.message;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006 by respective authors. All rights reserved.
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

import org.red5.server.service.PendingCall;

/**
 *
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */
public class RemotingCall extends PendingCall {

	public static final String HANDLER_SUCCESS = "/onResult";

	public static final String HANDLER_ERROR = "/onStatus";

	public String clientCallback = null;

	public RemotingCall(String serviceName, String serviceMethod,
			Object[] args, String callback) {
		super(serviceName, serviceMethod, args);
		setClientCallback(callback);
	}

	public void setClientCallback(String clientCallback) {
		this.clientCallback = clientCallback;
	}

	public String getClientResponse() {
		if (clientCallback != null) {
			return clientCallback
					+ (isSuccess() ? HANDLER_SUCCESS : HANDLER_ERROR);
		} else {
			return null;
		}
	}

	public Object getClientResult() {
		return isSuccess() ? getResult() : getException();
	}

}
