package org.red5.server.stream;

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

import org.red5.server.api.IBandwidthConfigure;
import org.red5.server.api.IFlowControllable;
import org.red5.server.api.stream.IClientStream;
import org.red5.server.api.stream.IStreamCapableConnection;

/**
 * Abstract base for client streams
 */
public abstract class AbstractClientStream extends AbstractStream implements
		IClientStream {

    /**
     *  Stream identifier. Unique across server.
     */
    private int streamId;
    /**
     *  Connection that works with streams
     */
	private IStreamCapableConnection conn;
    /**
     *  Bandwidth configuration
     */
	private IBandwidthConfigure bwConfig;
    /**
     *  Stream flow
     */
	private IStreamFlow streamFlow = new StreamFlow();

    /**
     * Return stream id
     * @return           Stream id
     */
	public int getStreamId() {
		return streamId;
	}

    /**
     * Return connection associated with stream
     * @return           Stream capable connection object
     */
	public IStreamCapableConnection getConnection() {
		return conn;
	}

    /**
     * Return stream bandwidth configuration
     * @return            Bandwidth config
     */
	public IBandwidthConfigure getBandwidthConfigure() {
		return bwConfig;
	}

    /**
     * Setter for bandwidth config
     * @param config              Bandwidth config
     */
	public void setBandwidthConfigure(IBandwidthConfigure config) {
		this.bwConfig = config;
	}

    /**
     * Return parent flow controllable object (bandwidth preferences holder)
     * @return          IFlowControllable object
     */
	public IFlowControllable getParentFlowControllable() {
		return conn;
	}

    /**
     * Setter for stream id
     * @param streamId       Stream id
     */
	public void setStreamId(int streamId) {
		this.streamId = streamId;
	}

    /**
     * Setter for stream capable connection
     * @param conn           IStreamCapableConnection object
     */
	public void setConnection(IStreamCapableConnection conn) {
		this.conn = conn;
	}

    /**
     * Setter fpr stream flow
     * @param streamFlow     IStreamFlow object
     */
	protected void setStreamFlow(IStreamFlow streamFlow) {
		this.streamFlow = streamFlow;
	}

    /**
     * Return stream flow
     * @return               IStreamFlow object
     */
	public IStreamFlow getStreamFlow() {
		return streamFlow;
	}

}
