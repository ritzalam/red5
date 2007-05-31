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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.red5.server.api.IConnection;
import org.red5.server.api.IScope;
import org.red5.server.api.listeners.IConnectionListener;
import org.red5.server.api.listeners.IScopeListener;
import org.red5.server.api.scheduling.IScheduledJob;
import org.red5.server.api.scheduling.ISchedulingService;
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
	
	/** Scheduling service to use. */
	private ISchedulingService schedulingService;
	
	/** Hold currently active connections. */
	private Map<Integer, IConnection> connections = new ConcurrentHashMap<Integer, IConnection>();
	
	/** Scheduled job id for periodic connection updates. */
	private String updateGlobalConnectionsJob;
	
	/**
	 * Create new listeners and associate with connection.
	 * 
	 * @param conn connection to associate with
	 */
	public AdminListeners(IConnection conn, ISchedulingService schedulingService) {
		this.conn = conn;
		this.schedulingService = schedulingService;
	}
	
	/**
	 * Release references to any objects.
	 */
	protected void cleanup() {
		conn = null;
		connections.clear();
		setConnectionUpdateInterval(0);
	}
	
	/** {@inheritDoc} */
	public void notifyScopeCreated(IScope scope) {
		final String path = AdminHelpers.getScopePath(scope);
		ServiceUtils.invokeOnConnection(conn, "notifyScopeCreated", new Object[]{path});
	}

	/** {@inheritDoc} */
	public void notifyScopeRemoved(IScope scope) {
		final String path = AdminHelpers.getScopePath(scope);
		ServiceUtils.invokeOnConnection(conn, "notifyScopeRemoved", new Object[]{path});
	}

	/** {@inheritDoc} */
	public void notifyConnected(IConnection conn) {
		ServiceUtils.invokeOnConnection(conn, "notifyConnected", new Object[]{AdminHelpers.getConnectionParams(conn)});
		connections.put(Integer.valueOf(conn.hashCode()), conn);
	}

	/** {@inheritDoc} */
	public void notifyDisconnected(IConnection conn) {
		ServiceUtils.invokeOnConnection(conn, "notifyDisconnected", new Object[]{AdminHelpers.getConnectionParams(conn)});
		connections.remove(Integer.valueOf(conn.hashCode()));
	}

	/**
	 * Set interval to update connection informations periodically.
	 * 
	 * @param interval interval in milliseconds
	 */
	protected synchronized void setConnectionUpdateInterval(int interval) {
		// Kill existing job
		if (updateGlobalConnectionsJob != null) {
			schedulingService.removeScheduledJob(updateGlobalConnectionsJob);
			updateGlobalConnectionsJob = null;
		}
		
		if (interval > 0) {
			updateGlobalConnectionsJob = schedulingService.addScheduledJob(interval, new UpdateConnectionsJob());
		}
	}
	
	/** Periodically update connection informations. */
	class UpdateConnectionsJob implements IScheduledJob {

		/**
		 * Send informations about currently active connections to the client.
		 */
		public void execute(ISchedulingService service) throws CloneNotSupportedException {
			List<Map<Object, Object>> informations = new ArrayList<Map<Object, Object>>();
			for (Map.Entry<Integer, IConnection> entry: connections.entrySet()) {
				final IConnection existing = entry.getValue();
				informations.add(AdminHelpers.getConnectionLiveParams(existing));
				existing.ping();
			}
			
			if (!informations.isEmpty() && conn.isConnected()) {
				ServiceUtils.invokeOnConnection(conn, "updateConnections", new Object[]{informations});
			}
		}
		
	}
	
}
