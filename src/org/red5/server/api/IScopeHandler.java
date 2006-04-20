package org.red5.server.api;

import org.red5.server.api.event.IEventHandler;
import org.red5.server.api.service.IServiceCall;

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
 * The Scope Handler controls actions performed against a scope object, and also
 * is notified of all events.
 * 
 * Gives fine grained control over what actions can be performed with the can*
 * methods. Allows for detailed reporting on what is happening within the scope
 * with the on* methods. This is the core interface users implement to create
 * applications.
 * 
 * The thread local connection is always available via the Red5 object within
 * these methods
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard (luke@codegent.com)
 */
public interface IScopeHandler extends IEventHandler {

	/**
	 * Called when a scope is created for the first time
	 * 
	 * @param scope
	 *            the new scope object
	 */
	boolean start(IScope scope);

	/**
	 * Called just before a scope is disposed
	 */
	void stop(IScope scope);
	
	/**
	 * Called just before every connection to a scope
	 * 
	 * @param conn
	 *            connection object
	 */
	/*boolean connect(IConnection conn, IScope scope);*/

	/**
	 * Called just before every connection to a scope
	 * 
	 * @param conn
	 *            connection object
	 */
	boolean connect(IConnection conn, IScope scope, Object[] params);

	/**
	 * Called just after the a connection is disconnected
	 * 
	 * @param conn
	 *            connection object
	 */
	void disconnect(IConnection conn, IScope scope);
	
	
	boolean addChildScope(IBasicScope scope);
	
	void removeChildScope(IBasicScope scope);

	/**
	 * Called just before a client enters the scope
	 * 
	 * @param client
	 *            client object
	 */
	boolean join(IClient client, IScope scope);

	/**
	 * Called just after the client leaves the scope
	 * 
	 * @param client
	 *            client object
	 */
	void leave(IClient client, IScope scope);

	/**
	 * Called when a service is called
	 * 
	 * @param call
	 *            the call object
	 */
	boolean serviceCall(IConnection conn, IServiceCall call);
	
}