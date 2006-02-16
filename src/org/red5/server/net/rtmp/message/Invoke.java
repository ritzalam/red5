package org.red5.server.net.rtmp.message;

import java.util.Map;

import org.red5.server.service.Call;

public class Invoke extends Message {
	
	private static final int INITIAL_CAPACITY = 1024;
	
	private Call call;
	private int invokeId = 0;
	private Map connectionParams = null;
	private boolean andReturn = true;
	
	public Invoke(){
		super(TYPE_INVOKE,INITIAL_CAPACITY);
	}

	public Call getCall() {
		return call;
	}

	public void setCall(Call call) {
		this.call = call;
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

	public boolean isAndReturn() {
		return andReturn;
	}

	public void setAndReturn(boolean andReturn) {
		this.andReturn = andReturn;
	}
	
	public byte getDataType(){
		return (andReturn) ? TYPE_INVOKE : TYPE_NOTIFY;
	}
	
	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append("Invoke: ").append(call);
		return sb.toString();
	}
	
}
