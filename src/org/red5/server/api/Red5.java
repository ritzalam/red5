package org.red5.server.api;

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
 * Utility class for accessing Red5 API objects.
 * 
 * This class uses a thread local, and will be setup by the service invoker.
 * 
 * The code below shows various uses.
 * <p>
 * <code>
 * IConnection conn = Red5.getConnectionLocal();<br />
 * Red5 r5 = new Red5();<br />
 * IScope scope = r5.getScope();<br />
 * conn = r5.getConnection();<br />
 * r5 = new Red5(conn);<br />
 * IClient client = r5.getClient();<br />
 * </code>
 * </p>
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard (luke@codegent.com)
 */
public final class Red5 {

	private static ThreadLocal<IConnection> connThreadLocal = new ThreadLocal<IConnection>();

	public IConnection conn = null;

	/**
	 * Create a new Red5 object using given connection
	 * 
	 * @param conn
	 *            connection object
	 */
	public Red5(IConnection conn) {
		this.conn = conn;
	}

	/**
	 * Create a new Red5 object using the connection local to the current thread
	 * A bit of magic that lets you access the red5 scope from anywhere
	 */
	public Red5() {
		conn = Red5.getConnectionLocal();
	}

	public static void setConnectionLocal(IConnection connection) {
		connThreadLocal.set(connection);
	}

	/**
	 * Get the connection associated with the current thread
	 * 
	 * @return connection object
	 */
	public static IConnection getConnectionLocal() {
		return connThreadLocal.get();
	}

	/**
	 * Get the connection object
	 * 
	 * @return connection object
	 */
	public IConnection getConnection() {
		return conn;
	}

	/**
	 * Get the scope
	 * 
	 * @return scope object
	 */
	public IScope getScope() {
		return conn.getScope();
	}

	/**
	 * Get the client
	 * 
	 * @return client object
	 */
	public IClient getClient() {
		return conn.getClient();
	}

	/**
	 * Get the spring application context
	 * 
	 * @return application context
	 */
	public IContext getContext() {
		return conn.getScope().getContext();
	}

}