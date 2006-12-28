package org.red5.server.net.rtmp.event;

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

/**
 * Server bandwidth event
 */
public class ServerBW extends BaseEvent {
    /**
     * Bandwidth
     */
	private int bandwidth;

    /**
     * Server bandwidth event
     * @param bandwidth      Bandwidth
     */
    public ServerBW(int bandwidth) {
		super(Type.STREAM_CONTROL);
		this.bandwidth = bandwidth;
	}

	/** {@inheritDoc} */
    @Override
	public byte getDataType() {
		return TYPE_SERVER_BANDWIDTH;
	}

	/**
     * Getter for bandwidth
     *
     * @return  Bandwidth
     */
    public int getBandwidth() {
		return bandwidth;
	}

	/**
     * Setter for bandwidth
     *
     * @param bandwidth  New bandwidth.
     */
    public void setBandwidth(int bandwidth) {
		this.bandwidth = bandwidth;
	}

	/** {@inheritDoc} */
    @Override
	public String toString() {
		return "ServerBW: " + bandwidth;
	}

	/** {@inheritDoc} */
    @Override
	protected void releaseInternal() {

	}

}
