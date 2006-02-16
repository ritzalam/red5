package org.red5.server.net.rtmp.message;

public class Notify extends Invoke {

	public Notify(){
		super();
		setDataType(TYPE_NOTIFY);
		setAndReturn(false);
	}
	
}
