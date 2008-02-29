package org.red5.server.net.mrtmp;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2008 by respective authors (see below). All rights reserved.
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

import org.red5.server.net.rtmp.RTMPConnection;

/**
 * @author Steven Gong (steven.gong@gmail.com)
 */
public interface IMRTMPManager {
	/**
	 * Map a client to an Edge/Origin MRTMP connection.
	 * On Edge, the server will find an Origin connection per routing logic.
	 * On Origin, the server will send to the original in-coming connection
	 * if the client connection type is persistent. Or the latest in-coming
	 * connection will be used.
	 * @param conn
	 * @return
	 */
	IMRTMPConnection lookupMRTMPConnection(RTMPConnection conn);
	
	/**
	 * Register a MRTMP connection so that it can be later
	 * been looked up.
	 * @param conn
	 * @return whether the registration is successful
	 */
	boolean registerConnection(IMRTMPConnection conn);
	
	/**
	 * Unregister a MRTMP connection.
	 * @param conn
	 * @return whether the registration is successful
	 */
	boolean unregisterConnection(IMRTMPConnection conn);
}
