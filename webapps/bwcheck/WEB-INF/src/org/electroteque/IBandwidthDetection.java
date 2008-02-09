package org.electroteque;

import org.red5.server.api.IConnection;

public interface IBandwidthDetection {
	public void checkBandwidth(IConnection p_client);
	public void calculateClientBw(IConnection p_client);
}