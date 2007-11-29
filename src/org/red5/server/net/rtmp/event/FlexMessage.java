package org.red5.server.net.rtmp.event;

/**
 * Flex method invocation. To be implemented.
 */
public class FlexMessage extends Invoke {
	private static final long serialVersionUID = 1854760132754344723L;

	public FlexMessage() {
		super();
	}
	
	/** {@inheritDoc} */
    @Override
	public byte getDataType() {
		return TYPE_FLEX_MESSAGE;
	}

}
