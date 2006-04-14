package org.red5.server.net.rtmp.message;

import org.red5.server.api.service.IPendingServiceCall;

public class Invoke extends Notify {
	
	public Invoke(){
		super();
		setDataType(TYPE_INVOKE);
	}

	public IPendingServiceCall getCall() {
		return (IPendingServiceCall) call;
	}

	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append("Invoke: ").append(call);
		return sb.toString();
	}
	
}
