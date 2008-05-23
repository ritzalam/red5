package org.red5.server.net.rtmps;

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

import java.util.List;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.SSLFilter;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.red5.server.net.protocol.ProtocolState;
import org.red5.server.net.rtmp.RTMPMinaConnection;
import org.red5.server.net.rtmp.RTMPMinaIoHandler;
import org.red5.server.net.rtmp.codec.RTMP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles all RTMPS protocol events fired by the MINA framework.
 */
public class RTMPSMinaIoHandler extends RTMPMinaIoHandler {
    /**
     * Logger
     */
	protected static Logger log = LoggerFactory.getLogger(RTMPSMinaIoHandler.class);

	/** {@inheritDoc} */
    @Override
	public void exceptionCaught(IoSession session, Throwable cause)	throws Exception {
   		log.debug("Exception caught {}", cause);
        cause.printStackTrace();
        session.close();   		
	}

    @Override
	public void messageReceived(IoSession session, Object in) throws Exception {
   		log.debug("messageRecieved");
		final ProtocolState state = (ProtocolState) session.getAttribute(ProtocolState.SESSION_KEY);
		if (in instanceof ByteBuffer) {
			rawBufferRecieved(state, (ByteBuffer) in, session);
			return;
		} else if (in instanceof SSLFilter.SSLFilterMessage) {
			log.debug("SSLFilterMessage received: {}", ((SSLFilter.SSLFilterMessage) in).toString());
			return;
		}
		final RTMPMinaConnection conn = (RTMPMinaConnection) session.getAttachment();
		handler.messageReceived(conn, state, in);
	}    
    
	/** {@inheritDoc} */
    @Override
	public void sessionCreated(IoSession session) throws Exception {
		log.debug("Session created");
		// moved protocol state from connection object to RTMP object
		RTMP rtmp = new RTMP(mode);
		session.setAttribute(ProtocolState.SESSION_KEY, rtmp);

		IoFilterChain chain = session.getFilterChain();
		if (log.isDebugEnabled()) {
    		for (IoFilterChain.Entry entry : (List<IoFilterChain.Entry>) chain.getAll()) {
    			log.debug("Filter entry: {} {}", entry.getName(), entry.getFilter().getClass().getName());
    		}
		}		
		chain.addLast("protocolFilter",	new ProtocolCodecFilter(codecFactory));
		
        session.setIdleTime(IdleStatus.BOTH_IDLE, 10);
        
        // Use SSL negotiation notification
        //session.setAttribute(SSLFilter.USE_NOTIFICATION);		
        
		RTMPMinaConnection conn = (RTMPMinaConnection) rtmpConnManager.createConnection(RTMPMinaConnection.class);
		conn.setIoSession(session);
		conn.setState(rtmp);
		session.setAttachment(conn);		
	}

    public void sessionIdle(IoSession session, IdleStatus status) {
        log.info("IDLE #{}", session.getIdleCount(IdleStatus.BOTH_IDLE));
    }    
    
}