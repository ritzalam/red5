package org.red5.server.api.stream;

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

import org.red5.server.api.IConnection;
import org.red5.server.api.IFlowControllable;

/**
 * A connection that supports streaming.
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard (luke@codegent.com)
 * @author Steven Gong (steven.gong@gmail.com)
 */
public interface IStreamCapableConnection extends IConnection, IFlowControllable {

	/**
	 * Return a reserved stream id for use.
	 * According to FCS/FMS regulation, the base is 1.
	 * @return
	 */
	int reserveStreamId();
	
	/**
	 * Unreserve this id for future use.
	 * @param streamId
	 */
	void unreserveStreamId(int streamId);
	
	/**
	 * Deletes the stream with the given id.
	 * 
	 * @param streamId
	 */
	void deleteStreamById(int streamId);
	
	/**
	 * Get a stream by its id.
	 * @param streamId
	 * @return
	 */
	IClientStream getStreamById(int streamId);

	/**
	 * Create a stream that can play only one item.
	 * @param streamId
	 * @return
	 */
	ISingleItemSubscriberStream newSingleItemSubscriberStream(int streamId);
	
	/**
	 * Create a stream that can play a list.
	 * @param streamId
	 * @return
	 */
	IPlaylistSubscriberStream newPlaylistSubscriberStream(int streamId);

	/**
	 * Create a broadcast stream.
	 * @param streamId
	 * @return
	 */
	IClientBroadcastStream newBroadcastStream(int streamId);

	/**
	 * Total number of video messages that are pending to be sent to a stream.
	 *
	 * @param streamId
	 * @return number of pending video messages
	 */
	long getPendingVideoMessages(int streamId);

}