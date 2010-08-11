package org.red5.server.net.filter;

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

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.core.write.WriteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link IoFilter} that limits bandwidth (bytes per second) related with
 * read and write operations on a per-session basis.
 * <p>
 * It is always recommended to add this filter in the first place of the
 * {@link IoFilterChain}.
 * 
 * <br />
 * This originated from the Mina sandbox.
 */
public class TrafficShapingFilter extends IoFilterAdapter {

	protected static Logger log = LoggerFactory.getLogger(TrafficShapingFilter.class);

	private final AttributeKey STATE = new AttributeKey(getClass(), "state");

	private final ScheduledExecutorService scheduledExecutor;

	private final MessageSizeEstimator messageSizeEstimator;

	private volatile int maxReadThroughput;

	private volatile int maxWriteThroughput;

	private volatile int poolSize = 1;

	public TrafficShapingFilter(int maxReadThroughput, int maxWriteThroughput) {
		this(null, null, maxReadThroughput, maxWriteThroughput);
	}

	public TrafficShapingFilter(ScheduledExecutorService scheduledExecutor, int maxReadThroughput,
			int maxWriteThroughput) {
		this(scheduledExecutor, null, maxReadThroughput, maxWriteThroughput);
	}

	public TrafficShapingFilter(ScheduledExecutorService scheduledExecutor, MessageSizeEstimator messageSizeEstimator,
			int maxReadThroughput, int maxWriteThroughput) {

		log.debug("ctor - executor: {} estimator: {} max read: {} max write: {}", new Object[] { scheduledExecutor,
				messageSizeEstimator, maxReadThroughput, maxWriteThroughput });

		if (scheduledExecutor == null) {
			scheduledExecutor = new ScheduledThreadPoolExecutor(poolSize);
			//throw new NullPointerException("scheduledExecutor");
		}

		if (messageSizeEstimator == null) {
			messageSizeEstimator = new DefaultMessageSizeEstimator() {
				@Override
				public int estimateSize(Object message) {
					if (message instanceof IoBuffer) {
						return ((IoBuffer) message).remaining();
					}
					return super.estimateSize(message);
				}
			};
		}

		this.scheduledExecutor = scheduledExecutor;
		this.messageSizeEstimator = messageSizeEstimator;
		setMaxReadThroughput(maxReadThroughput);
		setMaxWriteThroughput(maxWriteThroughput);
	}

	public ScheduledExecutorService getScheduledExecutor() {
		return scheduledExecutor;
	}

	public MessageSizeEstimator getMessageSizeEstimator() {
		return messageSizeEstimator;
	}

	public int getMaxReadThroughput() {
		return maxReadThroughput;
	}

	public void setMaxReadThroughput(int maxReadThroughput) {
		if (maxReadThroughput < 0) {
			maxReadThroughput = 0;
		}
		this.maxReadThroughput = maxReadThroughput;
	}

	public int getMaxWriteThroughput() {
		return maxWriteThroughput;
	}

	public void setMaxWriteThroughput(int maxWriteThroughput) {
		if (maxWriteThroughput < 0) {
			maxWriteThroughput = 0;
		}
		this.maxWriteThroughput = maxWriteThroughput;
	}

	public int getPoolSize() {
		return poolSize;
	}

	public void setPoolSize(int poolSize) {
		if (poolSize < 1) {
			poolSize = 1;
		}
		this.poolSize = poolSize;
	}

	@Override
	public void onPreAdd(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
		if (parent.contains(this)) {
			throw new IllegalArgumentException(
					"You can't add the same filter instance more than once.  Create another instance and add it.");
		}
		parent.getSession().setAttribute(STATE, new State());
		adjustReadBufferSize(parent.getSession());
	}

	@Override
	public void onPostRemove(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
		parent.getSession().removeAttribute(STATE);
	}

	@Override
	public void messageReceived(NextFilter nextFilter, final IoSession session, Object message) throws Exception {

		int maxReadThroughput = this.maxReadThroughput;
		//process the request if our max is greater than zero
		if (maxReadThroughput > 0) {
			final State state = (State) session.getAttribute(STATE);
			long currentTime = System.currentTimeMillis();

			long suspendTime = 0;
			boolean firstRead = false;
			synchronized (state) {
				state.readBytes += messageSizeEstimator.estimateSize(message);

				if (!state.suspendedRead) {
					if (state.readStartTime == 0) {
						firstRead = true;
						state.readStartTime = currentTime - 1000;
					}

					long throughput = (state.readBytes * 1000 / (currentTime - state.readStartTime));
					if (throughput >= maxReadThroughput) {
						suspendTime = Math.max(0, state.readBytes * 1000 / maxReadThroughput
								- (firstRead ? 0 : currentTime - state.readStartTime));

						state.readBytes = 0;
						state.readStartTime = 0;
						state.suspendedRead = suspendTime != 0;

						adjustReadBufferSize(session);
					}
				}
			}

			if (suspendTime != 0) {
				session.suspendRead();
				scheduledExecutor.schedule(new Runnable() {
					public void run() {
						synchronized (state) {
							state.suspendedRead = false;
						}
						session.resumeRead();
					}
				}, suspendTime, TimeUnit.MILLISECONDS);
			}
		}

		nextFilter.messageReceived(session, message);

	}

	private void adjustReadBufferSize(IoSession session) {
		int maxReadThroughput = this.maxReadThroughput;
		if (maxReadThroughput == 0) {
			return;
		}
		IoSessionConfig config = session.getConfig();
		if (config.getReadBufferSize() > maxReadThroughput) {
			config.setReadBufferSize(maxReadThroughput);
		}
		if (config.getMaxReadBufferSize() > maxReadThroughput) {
			config.setMaxReadBufferSize(maxReadThroughput);
		}
	}

	@Override
	public void messageSent(NextFilter nextFilter, final IoSession session, WriteRequest writeRequest) throws Exception {

		int maxWriteThroughput = this.maxWriteThroughput;
		//process the request if our max is greater than zero
		if (maxWriteThroughput > 0) {
			final State state = (State) session.getAttribute(STATE);
			long currentTime = System.currentTimeMillis();

			long suspendTime = 0;
			boolean firstWrite = false;
			synchronized (state) {
				state.writtenBytes += messageSizeEstimator.estimateSize(writeRequest.getMessage());
				if (!state.suspendedWrite) {
					if (state.writeStartTime == 0) {
						firstWrite = true;
						state.writeStartTime = currentTime - 1000;
					}

					long throughput = (state.writtenBytes * 1000 / (currentTime - state.writeStartTime));
					if (throughput >= maxWriteThroughput) {
						suspendTime = Math.max(0, state.writtenBytes * 1000 / maxWriteThroughput
								- (firstWrite ? 0 : currentTime - state.writeStartTime));

						state.writtenBytes = 0;
						state.writeStartTime = 0;
						state.suspendedWrite = suspendTime != 0;
					}
				}
			}

			if (suspendTime != 0) {
				log.trace("Suspending write");
				session.suspendWrite();
				scheduledExecutor.schedule(new Runnable() {
					public void run() {
						synchronized (state) {
							state.suspendedWrite = false;
						}
						session.resumeWrite();
						log.trace("Resuming write");
					}
				}, suspendTime, TimeUnit.MILLISECONDS);
			}
		}

		nextFilter.messageSent(session, writeRequest);

	}

	private static class State {
		private long readStartTime;

		private long writeStartTime;

		private boolean suspendedRead;

		private boolean suspendedWrite;

		private long readBytes;

		private long writtenBytes;
	}
}
