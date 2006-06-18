package org.red5.server.net.remoting;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright © 2006 by respective authors (see below). All rights reserved.
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

/**
 * Callback for asynchronous remoting calls.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 *
 */
public interface IRemotingCallback {

	/**
	 * The result of a remoting call has been received.
	 *  
	 * @param client
	 * @param method
	 * @param params
	 * @param result
	 */
	public void resultReceived(RemotingClient client, String method, Object[] params, Object result);
	
	/**
	 * An error occured while performing the remoting call.
	 * 
	 * @param client
	 * @param method
	 * @param params
	 * @param error
	 */
	public void errorReceived(RemotingClient client, String method, Object[] params, Throwable error);
	
}
