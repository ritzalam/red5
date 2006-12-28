package org.red5.server.net.rtmp.event;

/**
 * Client bandwidth event
 */
public class ClientBW extends BaseEvent {
    /**
     * Bandwidth
     */
	private int bandwidth;

    /**
     * /XXX : what is this?
     */
    private byte value2;

	public ClientBW(int bandwidth, byte value2) {
		super(Type.STREAM_CONTROL);
		this.bandwidth = bandwidth;
		this.value2 = value2;
	}

	/** {@inheritDoc} */
    @Override
	public byte getDataType() {
		return TYPE_CLIENT_BANDWIDTH;
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
     * Setter for bandwidth
     *
     * @param bandwidth  New bandwidth
     */
    public void setBandwidth(int bandwidth) {
		this.bandwidth = bandwidth;
	}

	/**
     * Getter for value2
     *
     * @return Value for property 'value2'.
     */
    public byte getValue2() {
		return value2;
	}

	/**
     * Setter for property 'value2'.
     *
     * @param value2 Value to set for property 'value2'.
     */
    public void setValue2(byte value2) {
		this.value2 = value2;
	}

	/** {@inheritDoc} */
    @Override
	public String toString() {
		return "ClientBW: " + bandwidth + " value2: " + value2;
	}

	/** {@inheritDoc} */
    @Override
	protected void releaseInternal() {

	}

}
