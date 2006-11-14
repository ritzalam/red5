package org.red5.server.stream;

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

import java.io.File;
import java.util.List;

import org.red5.server.api.IScope;
import org.red5.server.api.IScopeService;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.messaging.IMessageInput;

public interface IProviderService extends IScopeService {
	public static String BEAN_NAME = "providerService";

	/**
	 * Get a named provider as the source of input.
	 * Live stream first, VOD stream second.
	 * @param scope
	 * @param name
	 * @return <tt>null</tt> if nothing found.
	 */
	IMessageInput getProviderInput(IScope scope, String name);

	/**
	 * Get a named Live provider as the source of input.
	 * 
	 * @param scope
	 * @param name
	 * @return <tt>null</tt> if not found.
	 */
	IMessageInput getLiveProviderInput(IScope scope, String name,
			boolean needCreate);

	/**
	 * Get a named VOD provider as the source of input.
	 * 
	 * @param scope
	 * @param name
	 * @return <tt>null</tt> if not found.
	 */
	IMessageInput getVODProviderInput(IScope scope, String name);

	/**
	 * Get a named VOD source file.
	 * 
	 * @param scope
	 * @param name
	 * @return <tt>null</tt> if not found.
	 */
	File getVODProviderFile(IScope scope, String name);

	/**
	 * Register a broadcast stream to a scope.
	 * 
	 * @param scope
	 * @param name
	 * @param bs
	 * @return <tt>true</tt> if register successfully.
	 */
	boolean registerBroadcastStream(IScope scope, String name,
			IBroadcastStream bs);

	/**
	 * Get names of existing broadcast streams in a scope. 
	 * 
	 * @param scope
	 * @return list of stream names 
	 */
	List<String> getBroadcastStreamNames(IScope scope);

	/**
	 * Unregister a broadcast stream of a specific name from a scope.
	 * 
	 * @param scope
	 * @param name
	 * @return <tt>true</tt> if unregister successfully.
	 */
	boolean unregisterBroadcastStream(IScope scope, String name);
}
