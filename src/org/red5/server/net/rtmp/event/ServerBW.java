package org.red5.server.net.rtmp.event;

public class ServerBW extends BaseEvent {

	private int bandwidth = 0;

	public ServerBW(int bandwidth) {
		super(Type.STREAM_CONTROL);
		this.bandwidth = bandwidth;
	}

	/** {@inheritDoc} */
    @Override
	public byte getDataType() {
		return TYPE_SERVER_BANDWIDTH;
	}

	/**
     * Getter for property 'bandwidth'.
     *
     * @return Value for property 'bandwidth'.
     */
    public int getBandwidth() {
		return bandwidth;
	}

	/**
     * Setter for property 'bandwidth'.
     *
     * @param bandwidth Value to set for property 'bandwidth'.
     */
    public void setBandwidth(int bandwidth) {
		this.bandwidth = bandwidth;
	}

	/** {@inheritDoc} */
    @Override
	public String toString() {
		return "ServerBW: " + bandwidth;
	}

	/** {@inheritDoc} */
    @Override
	protected void releaseInternal() {

	}

}
