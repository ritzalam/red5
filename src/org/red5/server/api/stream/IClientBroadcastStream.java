package org.red5.server.api.stream;

import java.util.Map;

import org.red5.server.api.statistics.IClientBroadcastStreamStatistics;

/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright (c) 2006-2010 by respective authors (see below). All rights reserved.
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

/**
 * A broadcast stream that comes from client.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Steven Gong (steven.gong@gmail.com)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public interface IClientBroadcastStream extends IClientStream, IBroadcastStream {

	/**
	 * Notify client that stream is ready for publishing.
	 */
	public void startPublishing();
	
	/**
	 * Return statistics about the stream.
	 * 
	 * @return statistics
	 */
	public IClientBroadcastStreamStatistics getStatistics();

	/**
	 * Sets streaming parameters as supplied by the publishing application.
	 * 
	 * @param params
	 */
	public void setParameters(Map<String, String> params);
	
	/**
	 * Returns streaming parameters.
	 * 
	 * @return parameters
	 */
	public Map<String, String> getParameters();
	
}
