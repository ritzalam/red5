package org.red5.server.net.rtmp.event;

/**
 * Flex method invocation. To be implemented.
 */
public class FlexMessage extends Invoke {

	public FlexMessage() {
		super();
	}
	
	/** {@inheritDoc} */
    @Override
	public byte getDataType() {
		return TYPE_FLEX_MESSAGE;
	}

}
