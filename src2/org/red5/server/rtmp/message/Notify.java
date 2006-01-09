package org.red5.server.rtmp.message;

public class Notify extends Invoke {

	public Notify(){
		super();
		setDataType(TYPE_NOTIFY);
		setAndReturn(false);
	}
	
}
