/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2013 by respective authors (see below). All rights reserved.
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
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.api.Red5;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.servlet.ServletUtils;
import org.slf4j.Logger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * A RTMPT client / session.
 * 
 * @author The Red5 Project
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
	 * Timestamp of last data received on the connection
	 */
	private long tsLastDataReceived = 0;

	private AtomicLong lastBytesRead = new AtomicLong(0);

	private AtomicLong lastBytesWritten = new AtomicLong(0);

	/** Constructs a new RTMPTConnection */
	RTMPTConnection() {
		super(POLLING);
	}

	/**
	 * Creates a DummySession for this HTTP-based connection to allow our Mina based system happy.
	 * 
	 * @return session
	 */
	protected IoSession getSession() {
		IoSession session = new DummySession();
		session.setAttribute(RTMPConnection.RTMP_SESSION_ID, getSessionId());
		return session;
	}

	/** {@inheritDoc} */
	@Override
	protected void onInactive() {
		close();
	}

	/** {@inheritDoc} */
	@Override
	public void close() {
		log.debug("close {} state: {}", getSessionId(), state.states[state.getState()]);
		// ensure closing flag is set
		if (!isClosing()) {
			super.close();
			if (servlet != null) {
				servlet = null;
			}
			if (handler != null) {
				handler.connectionClosed(this);
			}
		}
	}

	/** {@inheritDoc} */
	@Override
	public boolean isIdle() {
		boolean inActivityExceeded = false;
		long lastTS = getLastDataReceived();
		long now = System.currentTimeMillis();
		long tsDelta = now - lastTS;
		if (lastTS > 0 && tsDelta > maxInactivity) {
			inActivityExceeded = true;
		}
		return inActivityExceeded || (isReaderIdle() && isWriterIdle());
	}

	/** {@inheritDoc} */
	@Override
	public boolean isReaderIdle() {
		// get the current bytes read on the connection
		long currentBytes = getReadBytes();
		// get our last bytes read
		long previousBytes = lastBytesRead.get();
		if (currentBytes > previousBytes) {
			log.trace("Client (read) is not idle");
			// client has sent data since last check and thus is not dead. No need to ping
			if (lastBytesRead.compareAndSet(previousBytes, currentBytes)) {
				return false;
			}
		}
		return true;
	}

	/** {@inheritDoc} */
	@Override
	public boolean isWriterIdle() {
		// get the current bytes written on the connection
		long currentBytes = getWrittenBytes();
		// get our last bytes written
		long previousBytes = lastBytesWritten.get();
		if (currentBytes > previousBytes) {
			log.trace("Client (write) is not idle");
			// server has sent data since last check
			if (lastBytesWritten.compareAndSet(previousBytes, currentBytes)) {
				return false;
			}
		}
		return true;
	}

	public void setRemoteAddress(String remoteAddress) {
		this.remoteAddress = remoteAddress;
	}

	public void setRemotePort(int remotePort) {
		this.remotePort = remotePort;
	}

	/** {@inheritDoc} */
	@Override
	public void setScheduler(ThreadPoolTaskScheduler scheduler) {
		super.setScheduler(scheduler);
		// outgoing queue processor
		scheduler.scheduleAtFixedRate(new ProcessTask(this), 250);
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
		if (request.getLocalPort() == 80) {
			host = request.getLocalName();
		} else {
			host = String.format("%s:%d", request.getLocalName(), request.getLocalPort());
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
	 * Register timestamp that data was received
	 */
	public void dataReceived() {
		tsLastDataReceived = System.currentTimeMillis();
	}

	/**
	 * Get the timestamp of last data received
	 * */
	public Long getLastDataReceived() {
		return tsLastDataReceived;
	}

	/**
	 * Processes queued incoming messages.
	 */
	private class ProcessTask implements Runnable {

		private final RTMPTConnection conn;

		ProcessTask(RTMPTConnection conn) {
			this.conn = conn;
		}

		public void run() {
			if (!pendingInMessages.isEmpty()) {
				// ensure the job is not already running
				if (running.compareAndSet(false, true)) {
					try {
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
					} catch (Exception e) {
						log.error("Error processing message: " + e.getMessage(), e);
					} finally {
						// reset run state
						running.set(false);
					}
				} else {
					log.trace("Process already running");
				}
			} else {
				log.trace("No incoming messages to process");
			}
		}

	}

}
