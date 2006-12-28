package org.red5.server.api.test;

import org.red5.server.BaseConnection;

public class TestConnection extends BaseConnection {

	public TestConnection(String host, String path, String sessionId) {
		super(PERSISTENT, host, null, 0, path, sessionId, null);
	}

    /**
     * Return encoding (currently AMF0)
     * @return          AMF0 encoding constant
     */
	public Encoding getEncoding() {
		return Encoding.AMF0;
	}
	/** {@inheritDoc} */
	@Override
	public long getReadBytes() {
		return 0;
	}

	/** {@inheritDoc} */
    @Override
	public long getWrittenBytes() {
		return 0;
	}

	/** {@inheritDoc} */
    public void ping() {

	}

	/** {@inheritDoc} */
    public int getLastPingTime() {
		return 0;
	}
}
