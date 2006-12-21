package org.red5.server.net.rtmp.event;

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
