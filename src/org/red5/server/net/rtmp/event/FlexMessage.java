package org.red5.server.net.rtmp.event;

/**
 * Flex method invocation. To be implemented.
 */
public class FlexMessage extends Invoke {

	public FlexMessage() {
		super();
	}
	
	@Override
	public byte getDataType() {
		// TODO Auto-generated method stub
		return TYPE_FLEX_MESSAGE;
	}

}
