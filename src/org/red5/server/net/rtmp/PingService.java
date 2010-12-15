package org.red5.server.net.rtmp;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.red5.server.api.scheduling.IScheduledJob;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.net.rtmp.event.Ping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PingService {
	private static Logger log = LoggerFactory.getLogger(PingService.class);
	
	private AtomicInteger pingRoundTripTime = new AtomicInteger(-1);
	private AtomicLong lastPingSentOn = new AtomicLong(0);
	private AtomicLong lastPongReceivedOn = new AtomicLong(0);

	private String connKeepAliveJobName;
	private volatile int pingInterval = 5000;
	private volatile int maxInactivity = 60000;
	
	private ISchedulingService schedulingService;
	private final RTMPConnection connection;
	
	public PingService(RTMPConnection connection) {
		this.connection = connection;
	}
	
	/** {@inheritDoc} */
	public void ping() {
		long newPingTime = System.currentTimeMillis();
		
		if (lastPingSentOn.get() == 0) {
			lastPongReceivedOn.set(newPingTime);
		}
		
		lastPingSentOn.set(newPingTime);
		int now = (int) (newPingTime & 0xffffffff);
		log.debug("Ping [clientId={} payload={} lastPingOn={}]", new Object[] { connection.getId(), (long)(now & 0xffffffffL), lastPingSentOn.get() });
		
		Ping pingRequest = new Ping();
		pingRequest.setEventType(Ping.PING_CLIENT);
		pingRequest.setValue2(now);
		connection.ping(pingRequest);
	}

	/**
	 * Marks that ping back was received.
	 * 
	 * @param pong
	 *            Ping object
	 */
	public void pingReceived(Ping pong) {
		long now = System.currentTimeMillis();
		lastPongReceivedOn.set(now);
		long pongPayload = (long)(0xFFFFFFFFL & pong.getValue2());
		long rtt = ((int)now & 0xFFFFFFFFL) - pongPayload;
		if (rtt < 0) {
			// Timestamp has wrapped around.
			rtt = (0xFFFFFFFF + now) - pongPayload;			
		} 
		pingRoundTripTime.set((int)rtt);		
		log.debug("Pong [clientId={} payload={} receivedOn={} rtt={}]", new Object[] { connection.getId(), pongPayload, now, rtt});		
	}

	/** {@inheritDoc} */
	public int getLastPingTime() {
		return pingRoundTripTime.get();
	}
	/**
	 * Setter for ping interval.
	 * 
	 * @param pingInterval Interval in ms to ping clients. Set to <code>0</code> to
	 *            disable ghost detection code.
	 */
	public void setPingInterval(int pingInterval) {
		this.pingInterval = pingInterval;
	}

	/**
	 * Setter for maximum inactivity.
	 * 
	 * @param maxInactivity Maximum time in ms after which a client is disconnected in
	 *            case of inactivity.
	 */
	public void setMaxInactivity(int maxInactivity) {
		this.maxInactivity = maxInactivity;
	}
	
	/**
	 * Starts measurement.
	 */
	public void startRoundTripMeasurement() {
		if (pingInterval > 0 && connKeepAliveJobName == null) {
			connKeepAliveJobName = schedulingService.addScheduledJob(pingInterval, new KeepAliveJob());
			log.debug("Keep alive job name {} for client id {}", connKeepAliveJobName, connection.getId());
		}
	}
	
	/**
	 * Sets the scheduling service.
	 * 
	 * @param schedulingService scheduling service
	 */
	public void setSchedulingService(ISchedulingService schedulingService) {
		this.schedulingService = schedulingService;
	}
	
	public void close() {
		if (connKeepAliveJobName != null) {
			schedulingService.removeScheduledJob(connKeepAliveJobName);
			connKeepAliveJobName = null;
		}
	}
	/**
	 * Quartz job that keeps connection alive and disconnects if client is dead.
	 */
	private class KeepAliveJob implements IScheduledJob {
		/** {@inheritDoc} */
		public void execute(ISchedulingService service) {
			long now = System.currentTimeMillis();
			if (lastPongReceivedOn.get() !=0 && (now - lastPongReceivedOn.get() > maxInactivity)) {
				log.debug("Keep alive job name {}", connKeepAliveJobName);
				if (log.isDebugEnabled()) {
					log.debug("Scheduled job list");
					for (String jobName : service.getScheduledJobNames()) {
						log.debug("Job: {}", jobName);
					}
				}
				service.removeScheduledJob(connKeepAliveJobName);
				connKeepAliveJobName = null;
				log.warn("Closing connection {}, [clientId={}, no reply to ping for {}ms, last ping sent {}ms ago]", new Object[] { connection, connection.getId(),
						(now - lastPongReceivedOn.get()), (now - lastPingSentOn.get()) });
				connection.onInactive();
				return;
			}
			
			/*
			 * Take ping RTT from the lastPong received until now. This allows us to increment RTT while waiting for the Pong. If the connection to the client
			 * is slow, this will allow us to throttle packets. When and if we receive the Pong, we get the real RTT.
			 */
			long rtt = now - lastPongReceivedOn.get();
			pingRoundTripTime.set((int)rtt);
			
			// Send ping command to client to trigger sending of data
			ping();
		}
	}
}
