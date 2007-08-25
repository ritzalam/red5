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

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.red5.server.adapter.ApplicationAdapter;
import org.red5.server.api.IConnection;
import org.red5.server.api.IGlobalScope;
import org.red5.server.api.IScope;
import org.red5.server.api.IServer;
import org.red5.server.api.Red5;
import org.red5.server.api.ScopeUtils;
import org.red5.server.api.listeners.IConnectionListener;
import org.red5.server.api.listeners.IScopeListener;
import org.red5.server.api.persistence.IPersistable;
import org.red5.server.api.statistics.IScopeStatistics;

/**
 * Main entry point for the admin application.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (bauch@struktur.de)
 */
public class Application extends ApplicationAdapter {

	/** Connection attribute keeping reference to listeners. */
	private static final String LISTENERS = IPersistable.TRANSIENT_PREFIX
			+ "_admin_listeners";

	/**
	 * Return the server instance for a given connection.
	 * 
	 * @param conn
	 *            the connection to return the server for
	 * @return the server
	 */
	public IServer getServer(IConnection conn) {
		// XXX: there probably should be an easier way to get the server
		final IGlobalScope scope = conn.getScope().getContext()
				.getGlobalScope();
		return scope.getServer();
	}

	/** {@inheritDoc} */
	@Override
	public boolean appConnect(IConnection conn, Object[] params) {
		if (!super.appConnect(conn, params))
			return false;

		final AdminListeners listeners = new AdminListeners(conn, this);
		conn.setAttribute(LISTENERS, listeners);

		final IServer server = getServer(conn);
		try {
			server.addListener((IScopeListener) listeners);
			server.addListener((IConnectionListener) listeners);
		} catch (RemoteException e) {
			log.warn("Error adding listeners on remote", e);
		}

		return true;
	}

	/** {@inheritDoc} */
	@Override
	public void appDisconnect(IConnection conn) {
		final AdminListeners listeners = (AdminListeners) conn
				.getAttribute(LISTENERS);
		if (listeners != null) {
			final IServer server = getServer(conn);
			try {
				server.removeListener((IScopeListener) listeners);
				server.removeListener((IConnectionListener) listeners);
			} catch (RemoteException e) {
				log.warn("Error removing listeners on remote", e);
			}
			conn.removeAttribute(LISTENERS);
			listeners.cleanup();
		}
		super.appDisconnect(conn);
	}

	/**
	 * Set interval to update connection informations periodically.
	 * 
	 * @param interval
	 *            interval in milliseconds
	 */
	public void setConnectionUpdateInterval(int interval) {
		IConnection conn = Red5.getConnectionLocal();
		final AdminListeners listeners = (AdminListeners) conn
				.getAttribute(LISTENERS);
		if (listeners != null) {
			listeners.setConnectionUpdateInterval(interval);
		}
	}

	/**
	 * Get global connection and client stats.
	 * 
	 * @return Map containing statistics
	 */
	public Map<String, Integer> getConnectionStats() {
		return getConnectionStats(null);
	}

	/**
	 * Get connection and client stats for a given path.
	 * 
	 * @param path
	 *            absolute path to return statistics for
	 * @return Map containing statistics
	 */
	@SuppressWarnings("unchecked")
	public Map<String, Integer> getConnectionStats(String path) {
		final IConnection conn = Red5.getConnectionLocal();
		IScope scope = conn.getScope().getContext().getGlobalScope();
		if (path != null && !"".equals(path)) {
			scope = ScopeUtils.resolveScope(scope, path);
		}

		if (scope == null) {
			// Requested path doesn't exist
			return Collections.EMPTY_MAP;
		}

		final IScopeStatistics stats = scope.getStatistics();
		if (stats == null) {
			// No statistics available
			return Collections.EMPTY_MAP;
		}

		final Map<String, Integer> result = new HashMap<String, Integer>();
		result.put("activeClients", stats.getActiveClients());
		result.put("activeConnections", stats.getActiveConnections());
		result.put("maxClients", stats.getMaxClients());
		result.put("maxConnections", stats.getMaxConnections());
		result.put("totalClients", stats.getTotalClients());
		result.put("totalConnections", stats.getTotalConnections());
		return result;
	}
}
