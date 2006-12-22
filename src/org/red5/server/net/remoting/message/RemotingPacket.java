package org.red5.server.net.remoting.message;

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

import java.nio.ByteBuffer;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

public class RemotingPacket {

	protected HttpServletRequest request;

	protected ByteBuffer data;

	protected List calls;

	protected String scopePath;

	public RemotingPacket(List calls) {
		this.calls = calls;
	}

	/**
     * Getter for property 'calls'.
     *
     * @return Value for property 'calls'.
     */
    public List getCalls() {
		return calls;
	}

	/**
     * Setter for property 'scopePath'.
     *
     * @param path Value to set for property 'scopePath'.
     */
    public void setScopePath(String path) {
		scopePath = path;
	}

	/**
     * Getter for property 'scopePath'.
     *
     * @return Value for property 'scopePath'.
     */
    public String getScopePath() {
		return scopePath;
	}

}
