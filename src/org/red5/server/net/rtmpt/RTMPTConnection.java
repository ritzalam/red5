/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2012 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.server.net.rtmpt;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.http.HttpServletRequest;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.api.Red5;
import org.red5.server.api.scheduling.IScheduledJob;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.net.protocol.ProtocolState;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.servlet.ServletUtils;
import org.slf4j.Logger;

/**
 * A RTMPT client / session.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 * @author Paul Gregoire (mondain@gmail.com)
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
	private volatile byte pollingDelay = INITIAL_POLLING_DELAY;

	/**
	 * Empty result counter, after reaching INCREASE_POLLING_DELAY_COUNT polling
	 * delay will increase
	 */
	private volatile long noPendingMessages;

	/**
	 * Servlet that created this connection.
	 */
	private RTMPTServlet servlet;

	/**
	 * Process job name
	 */
	private String processJobName;

	/**
	 * Process job run flag
	 */
	private final AtomicBoolean running;

	/** Constructs a new RTMPTConnection. */
	RTMPTConnection() {
		super(POLLING);
		state = new RTMP();
		running = new AtomicBoolean(false);
	}

	/**
	 * Creates a DummySession for this HTTP-based connection to allow our Mina based system happy.
	 * 
	 * @return session
	 */
	protected IoSession getSession() {
		IoSession session = new DummySession();
		session.setAttribute(RTMPConnection.RTMP_CONNECTION_KEY, this);
		session.setAttribute(ProtocolState.SESSION_KEY, getState());
		return session;
	}

	/** {@inheritDoc} */
	public void realClose() {
		log.debug("realClose connection id: {}", getId());
		// ensure closing flag is set
		if (!isClosing()) {
			close();
		}
		// remove the processing job
		schedulingService.removeScheduledJob(processJobName);
		// inform super that we need to close
		super.realClose();
		// inform the servlet
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

	/** {@inheritDoc} */
	@Override
	public boolean isReaderIdle() {
		return pendingInMessages.isEmpty();
	}

	/** {@inheritDoc} */
	@Override
	public boolean isWriterIdle() {
		return pendingOutMessages.isEmpty();
	}

	/** {@inheritDoc} */
	@Override
	public void setSchedulingService(ISchedulingService schedulingService) {
		this.schedulingService = schedulingService;
		processJobName = schedulingService.addScheduledJob(250, new ProcessJob(this));
	}

	/**
	 * Sets the client session id.
	 * 
	 * @param sessionId
	 */
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	/**
	 * Set the servlet that created the connection.
	 * 
	 * @param servlet
	 */
	protected void setServlet(RTMPTServlet servlet) {
		this.servlet = servlet;
	}

	/**
	 * Setter for servlet request.
	 * 
	 * @param request Servlet request
	 */
	public void setServletRequest(HttpServletRequest request) {
		host = request.getLocalName();
		// default http port isn't included in the host.
		if (request.getLocalPort() != 80) {
			host += ":" + request.getLocalPort();
		}
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
		log.trace("getPollingDelay {}", pollingDelay);
		log.trace("Polling delay: {} loops without messages: {}", pollingDelay, noPendingMessages);
		return (byte) (pollingDelay + 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IoBuffer getPendingMessages(int targetSize) {
		log.debug("Pending messages (in: {} out: {})", pendingInMessages.size(), pendingOutMessages.size());
		if (!pendingOutMessages.isEmpty()) {
			pollingDelay = INITIAL_POLLING_DELAY;
			noPendingMessages = 0;
		} else {
			noPendingMessages += 1;
			// if there are no pending outgoing adjust the polling delay
			if (noPendingMessages > INCREASE_POLLING_DELAY_COUNT) {
				if (pollingDelay == 0) {
					pollingDelay = 1;
				}
				pollingDelay = (byte) (pollingDelay * 2);
				if (pollingDelay > MAX_POLLING_DELAY) {
					pollingDelay = MAX_POLLING_DELAY;
				}
			}
		}
		return foldPendingMessages(targetSize);
	}

	/**
	 * Processes queued incoming messages.
	 */
	private class ProcessJob implements IScheduledJob {

		private final RTMPTConnection conn;

		ProcessJob(RTMPTConnection conn) {
			this.conn = conn;
		}

		/** {@inheritDoc} */
		public void execute(ISchedulingService service) {
			if (!pendingInMessages.isEmpty()) {
				// ensure the job is not already running
				if (running.compareAndSet(false, true)) {
					int available = pendingInMessages.size();
					log.debug("process - available: {}", available);
					// set connection local
					Red5.setConnectionLocal(conn);
					// get the session
					IoSession session = getSession();
					// grab some of the incoming data
					LinkedList<Object> sliceList = new LinkedList<Object>();
					int sliceSize = pendingInMessages.drainTo(sliceList, Math.min(maxInMessagesPerProcess, available));
					log.debug("processing: {}", sliceSize);
					// handle the messages
					for (Object message : sliceList) {
						try {
							handler.messageReceived(message, session);
						} catch (Exception e) {
							log.error("Could not process received message", e);
						}
						// exit execution of the parent connection is closing
						if (isClosing()) {
							break;
						}
					}
					// unset connection local
					Red5.setConnectionLocal(null);
					// reset run state
					running.compareAndSet(true, false);
				} else {
					log.trace("Process already running");
				}
			} else {
				log.trace("No incoming messages to process");
			}
		}

	}

}
