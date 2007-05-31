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

import java.util.HashMap;
import java.util.Map;

import org.red5.server.api.IConnection;
import org.red5.server.api.IScope;

/**
 * Helper functions for the admin application.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class AdminHelpers {

	/**
	 * Return dotted path for a given scope.
	 * 
	 * @param scope the scope to build the path for
	 * @return the dotted path
	 */
	public static String getScopePath(IScope scope) {
		StringBuilder builder = new StringBuilder();
		while (scope != null && scope.hasParent()) {
			builder.insert(0, scope.getName());
			if (scope.hasParent())
				builder.insert(0, "/");
			scope = scope.getParent();
		}
		return builder.toString();
	}
	
	/**
	 * Helper function to create maps easier.
	 * 
	 * @param args map entries, pass key/value pairs
	 * @return map
	 */
	private static Map<Object, Object> createMap(Object ... args) {
		assert args.length % 2 == 0;
		Map<Object, Object> result = new HashMap<Object, Object>();
		for (int i=0; i<args.length; i+=2) {
			result.put(args[i], args[i+1]);
		}
		return result;
	}
	
	/**
	 * Return static informations about a connection.
	 * 
	 * @param conn the connection to return informations for
	 * @return informations
	 */
	public static Map<Object, Object> getConnectionParams(IConnection conn) {
		final String path = getScopePath(conn.getScope());
		return createMap(
				"id", conn.hashCode(),
				"host", conn.getHost(),
				"path", path,
				"type", conn.getType().toString(),
				"encoding", conn.getEncoding().toString(),
				"remoteHost", conn.getRemoteAddress(),
				"remotePort", conn.getRemotePort(),
				"remoteAddresses", conn.getRemoteAddresses(),
				"params", conn.getConnectParams()
			);
	}
	
	/**
	 * Return live informations about a connection.
	 * 
	 * @param conn the connection to return informations for
	 * @return informations
	 */
	public static Map<Object, Object> getConnectionLiveParams(IConnection conn) {
		return createMap(
				"id", conn.hashCode(),
				"ping", conn.getLastPingTime(),
				"readBytes", conn.getReadBytes(),
				"writtenBytes", conn.getWrittenBytes(),
				"readMessages", conn.getReadMessages(),
				"writtenMessages", conn.getWrittenMessages(),
				"droppedMessages", conn.getDroppedMessages(),
				"pendingMessages", conn.getPendingMessages()
			);
	}
	

}
