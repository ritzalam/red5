package org.red5.server.stream;

import org.red5.server.api.stream.IBandwidthConfigure;
import org.red5.server.api.stream.IClientStream;
import org.red5.server.api.stream.IStreamCapableConnection;

public abstract class AbstractClientStream extends AbstractStream implements
		IClientStream {
	private int streamId;
	private IStreamCapableConnection conn;
	private IBandwidthConfigure bwConfig;

	public int getStreamId() {
		return streamId;
	}

	public IStreamCapableConnection getConnection() {
		return conn;
	}

	public IBandwidthConfigure getBandwidthConfigure() {
		return bwConfig;
	}

	public void setBandwidthConfigure(IBandwidthConfigure config) {
		this.bwConfig = config;
	}
	
	public void setStreamId(int streamId) {
		this.streamId = streamId;
	}
	
	public void setConnection(IStreamCapableConnection conn) {
		this.conn = conn;
	}
}
