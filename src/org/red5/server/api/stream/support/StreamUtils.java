package org.red5.server.api.stream.support;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2009 by respective authors (see below). All rights reserved.
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

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

import org.red5.server.api.IScope;
import org.red5.server.api.stream.IServerStream;
import org.red5.server.stream.ServerStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stream helper methods.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Steven Gong (steven.gong@gmail.com)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public abstract class StreamUtils {

	private static final Logger logger = LoggerFactory.getLogger(StreamUtils.class);

	/* Map to hold reference to the instanced server streams */
	private static volatile ConcurrentMap<String, IServerStream> serverStreamMap = new ConcurrentHashMap<String, IServerStream>();

	/**
	 * Creates server stream
	 * 
	 * @param scope Scope of stream
	 * @param name Name of stream
	 * @return		IServerStream object
	 */
	public static IServerStream createServerStream(IScope scope, String name) {
		logger.debug("Creating server stream: {} scope: {}", name, scope);
		ServerStream stream = new ServerStream();
		stream.setScope(scope);
		stream.setName(name);
		stream.setPublishedName(name);
		//save to the list for later lookups
		String key = scope.getName() + '/' + name;
		serverStreamMap.put(key, stream);
		return stream;
	}

	/**
	 * Looks up a server stream in the stream map. Null will be returned if the 
	 * stream is not found.
	 *
	 * @param scope Scope of stream
	 * @param name Name of stream
	 * @return		IServerStream object
	 */
	public static IServerStream getServerStream(IScope scope, String name) {
		logger.debug("Looking up server stream: {} scope: {}", name, scope);
		String key = scope.getName() + '/' + name;
		if (serverStreamMap.containsKey(key)) {
			return serverStreamMap.get(key);
		} else {
			logger.warn("Server stream not found with key: {}", key);
			return null;
		}
	}
	
	/**
	 * Puts a server stream in the stream map
	 *
	 * @param scope Scope of stream
	 * @param name Name of stream
	 * @param stream ServerStream object
	 */
	public static void putServerStream(IScope scope, String name, IServerStream stream) {
		logger.debug("Putting server stream in the map - name: {} scope: {} stream: {}", new Object[]{name, scope, stream});
		String key = scope.getName() + '/' + name;
		if (!serverStreamMap.containsKey(key)) {
			serverStreamMap.put(key, stream);
		} else {
			logger.warn("Server stream already exists in the map with key: {}", key);
		}
	}
	
	/**
	 * Removes a server stream from the stream map
	 *
	 * @param scope Scope of stream
	 * @param name Name of stream
	 */
	public static void removeServerStream(IScope scope, String name) {
		logger.debug("Removing server stream from the map - name: {} scope: {}", name, scope);
		String key = scope.getName() + '/' + name;
		if (serverStreamMap.containsKey(key)) {
			serverStreamMap.remove(key);
		} else {
			logger.warn("Server stream did not exist in the map with key: {}", key);
		}
	}	
	
}
