package org.red5.server.net.rtmp.message;

import java.util.Map;

import org.red5.server.api.service.IServiceCall;

public class Notify extends Message {

	private static final int INITIAL_CAPACITY = 1111;
	
	protected IServiceCall call = null;
	private int invokeId = 0;
	private Map connectionParams = null;
	
	public Notify(){
		super(TYPE_NOTIFY, INITIAL_CAPACITY);
	}

	public void setCall(IServiceCall call) {
		this.call = call;
	}
	
	public IServiceCall getCall() {
		return this.call;
	}
	
	public int getInvokeId() {
		return invokeId;
	}

	public void setInvokeId(int invokeId) {
		this.invokeId = invokeId;
	}

	protected void doRelease() {
		call = null;
	}

	public Map getConnectionParams() {
		return connectionParams;
	}

	public void setConnectionParams(Map connectionParams) {
		this.connectionParams = connectionParams;
	}

	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append("Notify: ").append(call);
		return sb.toString();
	}
	
}
