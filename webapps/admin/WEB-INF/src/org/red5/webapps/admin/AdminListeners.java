package org.red5.webapps.admin;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 *
 * Copyright (c) 2006-2007 by respective authors (see below). All rights reserved.
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

import org.red5.server.api.IConnection;
import org.red5.server.api.IScope;
import org.red5.server.api.listeners.IConnectionListener;
import org.red5.server.api.listeners.IScopeListener;
import org.red5.server.api.service.ServiceUtils;

/**
 * Listeners for the admin application.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class AdminListeners implements IScopeListener, IConnectionListener {

	/**
	 * Keep reference to creating connection. Don't prevent the connection
	 * from being released because of the listeners.
	 */
	private IConnection conn;
	
	/**
	 * Create new listeners and associate with connection.
	 * 
	 * @param conn connection to associate with
	 */
	public AdminListeners(IConnection conn) {
		setConnection(conn);
	}
	
	/**
	 * Set the connection to send notifications to.
	 * 
	 * @param conn the connection
	 */
	protected void setConnection(IConnection conn) {
		this.conn = conn;
	}
	
	/**
	 * Return dotted path for a given scope.
	 * 
	 * @param scope the scope to build the path for
	 * @return the dotted path
	 */
	private String getScopePath(IScope scope) {
		StringBuilder builder = new StringBuilder();
		while (scope != null && scope.hasParent()) {
			builder.insert(0, scope.getName());
			if (scope.hasParent())
				builder.insert(0, "/");
			scope = scope.getParent();
		}
		return builder.toString();
	}
	
	/** {@inheritDoc} */
	public void notifyScopeCreated(IScope scope) {
		final String path = getScopePath(scope);
		ServiceUtils.invokeOnConnection(conn, "notifyScopeCreated", new Object[]{path});
	}

	/** {@inheritDoc} */
	public void notifyScopeRemoved(IScope scope) {
		final String path = getScopePath(scope);
		ServiceUtils.invokeOnConnection(conn, "notifyScopeRemoved", new Object[]{path});
	}

	/**
	 * Return informations about a connection.
	 * 
	 * @param conn the connection to return informations for
	 * @return informations
	 */
	private Object[] getConnectionParams(IConnection conn) {
		final String path = getScopePath(conn.getScope());
		return new Object[] {
				conn.getHost(),
				path,
				conn.getType(),
				conn.getRemoteAddresses(),
				conn.getConnectParams()};
	}
	
	/** {@inheritDoc} */
	public void notifyConnected(IConnection conn) {
		ServiceUtils.invokeOnConnection(conn, "notifyConnected", getConnectionParams(conn));
	}

	/** {@inheritDoc} */
	public void notifyDisconnected(IConnection conn) {
		ServiceUtils.invokeOnConnection(conn, "notifyDisconnected", getConnectionParams(conn));
	}

}
