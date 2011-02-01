package org.red5.server.net.rtmpt;

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

import javax.servlet.http.HttpServletRequest;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.servlet.ServletUtils;
import org.slf4j.Logger;

/**
 * A RTMPT client / session.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class RTMPTConnection extends BaseRTMPTConnection {

	private static final Logger log = Red5LoggerFactory.getLogger(RTMPTConnection.class);

	/**
	 * Start to increase the polling delay after this many empty results
	 */
	private static final long INCREASE_POLLING_DELAY_COUNT = 10;

	/**
	 * Polling delay to start with.
	 */
	private static final byte INITIAL_POLLING_DELAY = 0;

	/**
	 * Maximum polling delay.
	 */
	private static final byte MAX_POLLING_DELAY = 32;

	/**
	 * Polling delay value
	 */
	private byte pollingDelay = INITIAL_POLLING_DELAY;

	/**
	 * Empty result counter, after reaching INCREASE_POLLING_DELAY_COUNT polling
	 * delay will increase
	 */
	private long noPendingMessages;

	/**
	 * Servlet that created this connection.
	 */
	private volatile RTMPTServlet servlet;

	/** Constructs a new RTMPTConnection. */
	RTMPTConnection() {
		super(POLLING);
		state = new RTMP(RTMP.MODE_SERVER);
		clientId = getNextClientId();
	}

	/**
	 * Set the servlet that created the connection.
	 * 
	 * @param servlet
	 */
	protected void setServlet(RTMPTServlet servlet) {
		this.servlet = servlet;
	}

	/** {@inheritDoc} */
	public void realClose() {
		super.realClose();
		if (servlet != null) {
			servlet.notifyClosed(this);
			servlet = null;
		}
	}

	/** {@inheritDoc} */
	@Override
	protected void onInactive() {
		log.debug("Inactive connection id: {}, closing", getId());
		close();
		realClose();
	}

	/**
	 * Setter for servlet request.
	 * 
	 * @param request Servlet request
	 */
	public void setServletRequest(HttpServletRequest request) {
		host = request.getLocalName();
		remoteAddress = request.getRemoteAddr();
		remoteAddresses = ServletUtils.getRemoteAddresses(request);
		remotePort = request.getRemotePort();
	}

	/**
	 * Return the polling delay to use.
	 * 
	 * @return the polling delay
	 */
	public byte getPollingDelay() {
		log.debug("getPollingDelay {}", pollingDelay);
		if (state.getState() == RTMP.STATE_DISCONNECTED) {
			// Special value to notify client about a closed connection.
			return (byte) 0;
		}
		return (byte) (pollingDelay + 1);
	}

	/**
	 * {@inheritDoc}
	 */
	public IoBuffer getPendingMessages(int targetSize) {
		long currentPendingMessages = getPendingMessages();
		if (currentPendingMessages == 0) {
			noPendingMessages += 1;
			if (noPendingMessages > INCREASE_POLLING_DELAY_COUNT) {
				if (pollingDelay == 0) {
					pollingDelay = 1;
				}
				pollingDelay = (byte) (pollingDelay * 2);
				if (pollingDelay > MAX_POLLING_DELAY) {
					pollingDelay = MAX_POLLING_DELAY;
				}
			}
			return null;
		}
		log.debug("Returning {} messages to client", currentPendingMessages);
		noPendingMessages = 0;
		pollingDelay = INITIAL_POLLING_DELAY;
		return foldPendingMessages(targetSize);
	}
}
