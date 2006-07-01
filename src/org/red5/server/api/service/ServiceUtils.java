package org.red5.server.api.service;

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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.red5.server.api.IConnection;
import org.red5.server.api.IScope;
import org.red5.server.api.Red5;

/**
 * Utility functions to invoke methods on connections.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 *
 */
public class ServiceUtils {

	/**
	 * Invoke a method on the current connection.
	 * 
	 * @param method
	 * 			name of the method to invoke
	 * @param params
	 * 			parameters to pass to the method
	 * @return <code>true</code> if the connection supports method calls, otherwise <code>false</code>
	 */
	public static boolean invokeOnConnection(String method, Object[] params) {
		return invokeOnConnection(method, params, null);
	}

	/**
	 * Invoke a method on the current connection and handle result.
	 * 
	 * @param method
	 * 			name of the method to invoke
	 * @param params
	 * 			parameters to pass to the method
	 * @param callback
	 * 			object to notify when result is received
	 * @return <code>true</code> if the connection supports method calls, otherwise <code>false</code>
	 */
	public static boolean invokeOnConnection(String method, Object[] params, IPendingServiceCallback callback) {
		IConnection conn = Red5.getConnectionLocal();
		return invokeOnConnection(conn, method, params, callback);
		
	}

	/**
	 * Invoke a method on a given connection.
	 * 
	 * @param conn
	 * 			connection to invoke method on
	 * @param method
	 * 			name of the method to invoke
	 * @param params
	 * 			parameters to pass to the method
	 * @return <code>true</code> if the connection supports method calls, otherwise <code>false</code>
	 */
	public static boolean invokeOnConnection(IConnection conn, String method, Object[] params) {
		return invokeOnConnection(conn, method, params, null);
	}

	/**
	 * Invoke a method on a given connection and handle result.
	 * 
	 * @param conn
	 * 			connection to invoke method on
	 * @param method
	 * 			name of the method to invoke
	 * @param params
	 * 			parameters to pass to the method
	 * @param callback
	 * 			object to notify when result is received
	 * @return <code>true</code> if the connection supports method calls, otherwise <code>false</code>
	 */
	public static boolean invokeOnConnection(IConnection conn, String method, Object[] params, IPendingServiceCallback callback) {
		if (conn instanceof IServiceCapableConnection) {
			if (callback == null)
				((IServiceCapableConnection) conn).invoke(method, params);
			else
				((IServiceCapableConnection) conn).invoke(method, params, callback);
			return true;
		} else
			return false;
	}

	/**
	 * Invoke a method on all connections to the current scope.
	 * 
	 * @param method
	 * 			name of the method to invoke
	 * @param params
	 * 			parameters to pass to the method
	 */
	public static void invokeOnAllConnections(String method, Object[] params) {
		invokeOnAllConnections(method, params, null);
	}

	/**
	 * Invoke a method on all connections to the current scope and handle result.
	 * 
	 * @param method
	 * 			name of the method to invoke
	 * @param params
	 * 			parameters to pass to the method
	 * @param callback
	 * 			object to notify when result is received
	 */
	public static void invokeOnAllConnections(String method, Object[] params, IPendingServiceCallback callback) {
		IScope scope = Red5.getConnectionLocal().getScope();
		invokeOnAllConnections(scope, method, params, callback);
	}

	/**
	 * Invoke a method on all connections to a given scope.
	 * 
	 * @param scope
	 * 			scope to get connections for
	 * @param method
	 * 			name of the method to invoke
	 * @param params
	 * 			parameters to pass to the method
	 */
	public static void invokeOnAllConnections(IScope scope, String method, Object[] params) {
		invokeOnAllConnections(scope, method, params, null);
	}

	/**
	 * Invoke a method on all connections to a given scope and handle result.
	 * 
	 * @param scope
	 * 			scope to get connections for
	 * @param method
	 * 			name of the method to invoke
	 * @param params
	 * 			parameters to pass to the method
	 * @param callback
	 * 			object to notify when result is received
	 */
	public static void invokeOnAllConnections(IScope scope, String method, Object[] params, IPendingServiceCallback callback) {
		List<IConnection> connections = new ArrayList<IConnection>();
		Iterator<IConnection> iter = scope.getConnections();
		while (iter.hasNext())
			connections.add(iter.next());
		
		if (callback == null)
			for (IConnection conn: connections)
				invokeOnConnection(conn, method, params);
		else
			for (IConnection conn: connections)
				invokeOnConnection(conn, method, params, callback);
	}
}
