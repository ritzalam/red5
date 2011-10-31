package org.red5.server.stream.bandwidth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;
import org.red5.server.api.service.IServiceCapableConnection;
import org.red5.server.api.stream.IStreamCapableConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Dan Rossi
 */
public class ServerClientDetection implements IPendingServiceCallback, IBandwidthDetection {

	private static Logger log = LoggerFactory.getLogger(ServerClientDetection.class);

	private IConnection client = null;

	private double latency = 0;

	private double cumLatency = 1;

	private int count = 0;

	private int sent = 0;

	private double kbitDown = 0;

	@SuppressWarnings("unused")
	private double kbitUp = 0;

	private double deltaDown = 0;

	@SuppressWarnings("unused")
	private double deltaUp = 0;

	private double deltaTime = 0;

	private Long timePassed;

	private List<Long> pakSent = new ArrayList<Long>();

	private List<Long> pakRecv = new ArrayList<Long>();

	private Map<String, Long> beginningValues;

	private double[] payload = new double[1200];

	private double[] payload_1 = new double[12000];

	private double[] payload_2 = new double[12000];

	public void checkBandwidth(IConnection conn) {
		calculateClientBw(conn);
	}

	public void calculateClientBw(IConnection conn) {
		for (int i = 0; i < 1200; i++) {
			payload[i] = Math.random();
		}
		conn.setAttribute("payload", payload);
		for (int i = 0; i < 12000; i++) {
			payload_1[i] = Math.random();
		}
		conn.setAttribute("payload_1", payload_1);
		for (int i = 0; i < 12000; i++) {
			payload_2[i] = Math.random();
		}
		conn.setAttribute("payload_2", payload_2);
		final IStreamCapableConnection beginningStats = this.getStats();
		final Long start = new Long(System.nanoTime() / 1000000);
		client = conn;
		beginningValues = new HashMap<String, Long>();
		beginningValues.put("b_down", beginningStats.getWrittenBytes());
		beginningValues.put("time", start);
		pakSent.add(start);
		sent++;
		log.debug("Starting bandwidth check {} ", new Object[] { start });
		callBWCheck("");
	}

	/**
	 * Handle callback from service call.
	 */
	public void resultReceived(IPendingServiceCall call) {
		Long now = new Long(System.nanoTime() / 1000000); 
		pakRecv.add(now);
		timePassed = (now - beginningValues.get("time"));
		count++;
		log.debug("count: {} sent: {} timePassed: {} latency: {}", new Object[] { count, sent, timePassed, latency });
		if (count == 1) {
			latency = Math.min(timePassed, 800);
			latency = Math.max(latency, 10);
			// We now have a latency figure so can start sending test data.
			// Second call. 1st packet sent
			pakSent.add(now);
			sent++;
			log.debug("Sending First Payload at {} count: {} sent: {} ", new Object[] { now, count, sent });
			callBWCheck(client.getAttribute("payload"));
		}
		//To run a very quick test, uncomment the following if statement and comment out the next 3 if statements.
		/*
		 * else if (count == 2 && (timePassed < 2000)) { pakSent.add(now);
		 * sent++; cumLatency++;
		 * this.callBWCheck(this.client.getAttribute("payload")); }
		 */
		//The following will progressively increase the size of the packets been sent until 1 second has elapsed.
		else if ((count > 1 && count < 3) && (timePassed < 1000)) {
			pakSent.add(now);
			sent++;
			cumLatency++;
			log.debug("Sending Second Payload at {} count: {} sent: {} ", new Object[] { now, count, sent });
			callBWCheck(client.getAttribute("payload_1"));
		} else if ((count >= 3 && count < 6) && (timePassed < 1000)) {
			pakSent.add(now);
			sent++;
			cumLatency++;
			log.debug("Sending Third Payload at {} count: {} sent: {} ", new Object[] { now, count, sent });
			callBWCheck(client.getAttribute("payload_1"));
		} else if (count >= 6 && (timePassed < 1000)) {
			pakSent.add(now);
			sent++;
			cumLatency++;
			log.debug("Sending Fourth Payload at {} count: {} sent: {}", new Object[] { now, count, sent });
			callBWCheck(client.getAttribute("payload_2"));
		}
		// Time elapsed now do the calcs
		else if (sent == count) {
			// see if we need to normalize latency
			if (latency >= 100) {
				// make sure satellite and modem is detected properly
				if (pakRecv.get(1) - pakRecv.get(0) > 1000) {
					latency = 100;
				}
			}
			client.removeAttribute("payload");
			client.removeAttribute("payload_1");
			client.removeAttribute("payload_2");
			final IStreamCapableConnection endStats = this.getStats();
			// bytes to kbits
			deltaDown = (endStats.getWrittenBytes() - beginningValues.get("b_down")) * 8 / 1000; 
			// total dl time - latency for each packet sent in secs
			deltaTime = ((now - beginningValues.get("time")) - (latency * cumLatency)) / 1000; 
			if (Math.round(deltaTime) <= 0) {
				deltaTime = (now - beginningValues.get("time") + latency) / 1000;
			}
			this.kbitDown = Math.round(deltaDown / deltaTime); // kbits / sec
			if (kbitDown < 100) {
				kbitDown = 100;
			}
			log.debug("onBWDone: kbitDown: {} deltaDown: {} deltaTime: {} latency: {} ", new Object[] { kbitDown, deltaDown, deltaTime, this.latency });
			callBWDone();
		}
	}

	private void callBWCheck(Object payload) {
		IConnection conn = Red5.getConnectionLocal();
		Map<String, Object> statsValues = new HashMap<String, Object>();
		statsValues.put("count", count);
		statsValues.put("sent", sent);
		statsValues.put("timePassed", timePassed);
		statsValues.put("latency", latency);
		statsValues.put("cumLatency", cumLatency);
		statsValues.put("payload", payload);
		if (conn instanceof IServiceCapableConnection) {
			((IServiceCapableConnection) conn).invoke("onBWCheck", new Object[] { statsValues }, this);
		}
	}

	private void callBWDone() {
		IConnection conn = Red5.getConnectionLocal();
		Map<String, Object> statsValues = new HashMap<String, Object>();
		statsValues.put("kbitDown", kbitDown);
		statsValues.put("deltaDown", deltaDown);
		statsValues.put("deltaTime", deltaTime);
		statsValues.put("latency", latency);
		if (conn instanceof IServiceCapableConnection) {
			((IServiceCapableConnection) conn).invoke("onBWDone", new Object[] { statsValues });
		}
	}

	private IStreamCapableConnection getStats() {
		IConnection conn = Red5.getConnectionLocal();
		if (conn instanceof IStreamCapableConnection) {
			return (IStreamCapableConnection) conn;
		}
		return null;
	}

	public void onServerClientBWCheck() {
		IConnection conn = Red5.getConnectionLocal();
		this.calculateClientBw(conn);
	}

}
