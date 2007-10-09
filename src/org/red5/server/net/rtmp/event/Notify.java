package org.red5.server.net.rtmp.event;

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

import java.util.Map;

import org.apache.mina.common.ByteBuffer;
import org.red5.server.api.service.IServiceCall;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.stream.IStreamData;

/**
 * Stream notification event
 */
public class Notify extends BaseEvent implements IStreamData, IStreamPacket {
    /**
     * Service call
     */
	protected IServiceCall call;
    /**
     * Event data
     */
	protected ByteBuffer data;
    /**
     * Invoke id
     */
	private int invokeId = 0;

    /**
     * Connection parameters
     */
    private Map connectionParams;

	/** Constructs a new Notify. */
    public Notify() {
		super(Type.SERVICE_CALL);
	}

    /**
     * Create new notification event with given byte buffer
     * @param data       Byte buffer
     */
    public Notify(ByteBuffer data) {
		super(Type.STREAM_DATA);
		this.data = data;
	}

    /**
     * Create new notification event with given service call
     * @param call        Service call
     */
	public Notify(IServiceCall call) {
		super(Type.SERVICE_CALL);
		this.call = call;
	}

	/** {@inheritDoc} */
    @Override
	public byte getDataType() {
		return TYPE_NOTIFY;
	}

	/**
     * Setter for data
     *
     * @param data  Data
     */
    public void setData(ByteBuffer data) {
		this.data = data;
	}

	/**
     * Setter for call
     *
     * @param call Service call
     */
    public void setCall(IServiceCall call) {
		this.call = call;
	}

	/**
     * Getter for service call
     *
     * @return  Service call
     */
    public IServiceCall getCall() {
		return this.call;
	}

	/** {@inheritDoc} */
    public ByteBuffer getData() {
		return data;
	}

	/**
     * Getter for invoke id
     *
     * @return  Invoke id
     */
    public int getInvokeId() {
		return invokeId;
	}

	/**
     * Setter for invoke id
     *
     * @param invokeId  Invoke id
     */
    public void setInvokeId(int invokeId) {
		this.invokeId = invokeId;
	}

    /**
     * Release event (nullify call object)
     */
    protected void doRelease() {
		call = null;
	}

	/**
     * Getter for connection parameters
     *
     * @return Connection parameters
     */
    public Map getConnectionParams() {
		return connectionParams;
	}

	/**
     * Setter for connection parameters
     *
     * @param connectionParams  Connection parameters
     */
    public void setConnectionParams(Map connectionParams) {
		this.connectionParams = connectionParams;
	}

	/** {@inheritDoc} */
    @Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Notify: ").append(call);
		return sb.toString();
	}

	/** {@inheritDoc} */
    @Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Notify)) {
			return false;
		}
		Notify other = (Notify) obj;
		if (getConnectionParams() == null
				&& other.getConnectionParams() != null) {
			return false;
		}
		if (getConnectionParams() != null
				&& other.getConnectionParams() == null) {
			return false;
		}
		if (getConnectionParams() != null
				&& !getConnectionParams().equals(other.getConnectionParams())) {
			return false;
		}
		if (getInvokeId() != other.getInvokeId()) {
			return false;
		}
		if (getCall() == null && other.getCall() != null) {
			return false;
		}
		if (getCall() != null && other.getCall() == null) {
			return false;
		}
		if (getCall() != null && !getCall().equals(other.getCall())) {
			return false;
		}
		return true;
	}

	/** {@inheritDoc} */
    @Override
	protected void releaseInternal() {
		if (data != null) {
			data = null;
		}
	}

}