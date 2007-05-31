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

import org.red5.server.adapter.ApplicationAdapter;
import org.red5.server.api.IConnection;
import org.red5.server.api.IGlobalScope;
import org.red5.server.api.IServer;
import org.red5.server.api.listeners.IConnectionListener;
import org.red5.server.api.listeners.IScopeListener;
import org.red5.server.api.persistence.IPersistable;

/**
 * Main entry point for the admin application.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (bauch@struktur.de)
 */
public class Application extends ApplicationAdapter {

	/** Connection attribute keeping reference to listeners. */
	private static final String LISTENERS = IPersistable.TRANSIENT_PREFIX + "_admin_listeners";
	
	/**
	 * Return the server instance for a given connection.
	 * 
	 * @param conn the connection to return the server for
	 * @return the server
	 */
	public IServer getServer(IConnection conn) {
		// XXX: there probably should be an easier way to get the server
		final IGlobalScope scope = conn.getScope().getContext().getGlobalScope();
		return scope.getServer();
	}
	
	/** {@inheritDoc} */
	@Override
	public boolean appConnect(IConnection conn, Object[] params) {
		if (!super.appConnect(conn, params))
			return false;
		
		final AdminListeners listeners = new AdminListeners(conn);
		conn.setAttribute(LISTENERS, listeners);
		
		final IServer server = getServer(conn);
		server.addListener((IScopeListener) listeners);
		server.addListener((IConnectionListener) listeners);
		
		return true;
	}
	
	/** {@inheritDoc} */
	@Override
	public void appDisconnect(IConnection conn) {
		final AdminListeners listeners = (AdminListeners) conn.getAttribute(LISTENERS);
		if (listeners != null) {
			final IServer server = getServer(conn);
			server.removeListener((IScopeListener) listeners);
			server.removeListener((IConnectionListener) listeners);
			conn.removeAttribute(LISTENERS);
			listeners.setConnection(null);
		}
		super.appDisconnect(conn);
	}
	
}
