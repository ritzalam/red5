package org.red5.server.net.rtmp.event;

public class ClientBW extends BaseEvent {

	private int bandwidth = 0;

	private byte value2 = 0;

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
     * Setter for property 'bandwidth'.
     *
     * @param bandwidth Value to set for property 'bandwidth'.
     */
    public void setBandwidth(int bandwidth) {
		this.bandwidth = bandwidth;
	}

	/**
     * Getter for property 'value2'.
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
