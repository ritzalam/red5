package org.red5.server.api.plugin;

/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright (c) 2006-2011 by respective authors (see below). All rights reserved.
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

import org.red5.server.Server;
import org.springframework.context.ApplicationContext;

/**
 * Base interface for a Red5 server Plug-in.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public interface IRed5Plugin {
	
	/**
	 * Returns a name / identifier for the plug-in.
	 * 
	 * @return plug-in's name
	 */
	String getName();
	
	/**
	 * Sets the top-most ApplicationContext within Red5.
	 * 
	 * @param context
	 */
	void setApplicationContext(ApplicationContext context);	
	
	/**
	 * Sets a reference to the server.
	 * 
	 * @param server
	 */
	void setServer(Server server);

	/**
	 * Lifecycle method called when the plug-in is started.
	 * 
	 * @throws Exception 
	 */
	void doStart() throws Exception;
		
	/**
	 * Lifecycle method called when the plug-in is stopped.
	 * 
	 * @throws Exception 
	 */
	void doStop() throws Exception;

}