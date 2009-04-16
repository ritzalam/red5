package org.red5.server.net.rtmp;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2009 by respective authors (see below). All rights reserved.
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

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.net.mrtmp.IMRTMPConnection;
import org.red5.server.net.mrtmp.IMRTMPOriginManager;
import org.red5.server.net.mrtmp.OriginMRTMPHandler;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.message.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A pseudo-connection on Origin that represents a client
 * on Edge.
 *
 * The connection is created behind a MRTMP connection so
 * no handshake job or keep-alive job is necessary. No raw byte
 * data write is needed either.
 *
 * @author Steven Gong (steven.gong@gmail.com)
 * @version $Id$
 */
public class RTMPOriginConnection extends RTMPConnection {
	
	private static final Logger log = LoggerFactory.getLogger(RTMPOriginConnection.class);
	
	private int ioSessionId;
	private IMRTMPOriginManager mrtmpManager;
	private OriginMRTMPHandler handler;
	private RTMP state;

	public RTMPOriginConnection(String type, int clientId) {
		this(type, clientId, 0);
	}

	public RTMPOriginConnection(String type, int clientId, int ioSessionId) {
		super(type);
		setId(clientId);
		this.ioSessionId = ioSessionId;
		state = new RTMP(RTMP.MODE_SERVER);
		state.setState(RTMP.STATE_CONNECTED);
	}
	
	public int getIoSessionId() {
		return ioSessionId;
	}

	public void setMrtmpManager(IMRTMPOriginManager mrtmpManager) {
		this.mrtmpManager = mrtmpManager;
	}

	public void setHandler(OriginMRTMPHandler handler) {
		this.handler = handler;
	}

	public RTMP getState() {
		return state;
	}

	@Override
	protected void onInactive() {
		// Edge already tracks the activity
		// no need to do again here.
	}

	@Override
	public void rawWrite(IoBuffer out) {
		// won't write any raw data on the wire
		// XXX should we throw exception here
		// to indicate an abnormal state ?
		log.warn("Erhhh... Raw write. Shouldn't be in here!");
	}

	@Override
	public void write(Packet packet) {
		IMRTMPConnection conn = mrtmpManager.lookupMRTMPConnection(this);
		if (conn == null) {
			// the connect is gone
			log.debug("Client {} is gone!", getId());
			return;
		}
		if (!type.equals(PERSISTENT)) {
			mrtmpManager.associate(this, conn);
		}
		log.debug("Origin writing packet to client {}:{}", getId(), packet.getMessage());
		conn.write(getId(), packet);
	}

	@Override
	public void startRoundTripMeasurement() {
		// Edge already tracks the RTT
		// no need to track RTT here.
	}

	@Override
	protected void startWaitForHandshake(ISchedulingService service) {
		// no handshake in MRTMP, simply ignore
	}

	@Override
	synchronized public void close() {
		if (state.getState() == RTMP.STATE_DISCONNECTED) {
			return;
		}
		IMRTMPConnection conn = mrtmpManager.lookupMRTMPConnection(this);
		if (conn != null) {
			conn.disconnect(getId());
		}
		handler.closeConnection(this);
	}
	
	synchronized public void realClose() {
		if (state.getState() != RTMP.STATE_DISCONNECTED) {
			state.setState(RTMP.STATE_DISCONNECTED);
			super.close();
		}
	}
	
}
