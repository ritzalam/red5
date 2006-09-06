package org.red5.server.adapter;

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

import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.IScope;

public interface IApplication  {
	
	/**
	 * Called once when application or room starts
	 * 
	 * @param app	Application or room level scope. See {@link org.red5.server.api.IScope} for details
	 * @return		<code>true</code> continues application run, <code>false</code> terminates
	 */
	public boolean appStart(IScope app);
	
	/**
	 * Called per each client connect
	 * 
	 * @param conn		{@link org.red5.server.api.IScope}
	 * @param params	List of params sent from client with NetConnection.connect call
	 * @return			<code>true</code> accepts the connection, <code>false</code> rejects it
	 */
	public boolean appConnect(IConnection conn, Object[] params);
	
	/**
	 * Called every time client joins app level scope
	 * 
	 * @param client
	 * @param app
	 * @return
	 */
	public boolean appJoin(IClient client, IScope app);
	
	/**
	 * Called every time client disconnects from the application
	 * 
	 * @param conn
	 */
	public void appDisconnect(IConnection conn);
	
	/**
	 * Called every time client leaves the application scope
	 * 
	 * @param client
	 * @param app
	 */
	public void appLeave(IClient client, IScope app);
	
	/**
	 * Called on application stop
	 * 
	 * @param app
	 */
	public void appStop(IScope app);
	
	/**
	 * Called on application room start
	 * 
	 * @param room
	 * @return
	 */
	public boolean roomStart(IScope room);
	
	/**
	 * Called every time client connects to the room
	 * 
	 * @param conn
	 * @return
	 */
	public boolean roomConnect(IConnection conn, Object[] params);
	
	/**
	 * Called when user joins room scope
	 * 
	 * @param client
	 * @param room
	 * @return
	 */
	public boolean roomJoin(IClient client, IScope room);
	
	/**
	 * Called when client disconnects from room  scope
	 * 
	 * @param conn
	 */
	public void roomDisconnect(IConnection conn);
	
	/**
	 * Called when user leaves room scope
	 * 
	 * @param client
	 * @param room
	 */
	public void roomLeave(IClient client, IScope room);
	
	/**
	 * Called on room scope stop
	 * 
	 * @param room
	 */
	public void roomStop(IScope room);
	
}
