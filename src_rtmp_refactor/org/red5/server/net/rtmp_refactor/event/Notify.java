package org.red5.server.net.rtmp_refactor.event;

import java.util.EventObject;
import java.util.Map;

import org.red5.server.api.service.IServiceCall;

public class Notify {
	
	protected IServiceCall call = null;
	private int invokeId = 0;
	private Map connectionParams = null;
	
	public Notify(){
	}

	public Notify(IServiceCall call){
		this.call = call;
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
	
	public boolean equals(Object obj){
		if(obj == null) return false;
		if(!(obj instanceof Notify)) return false;
		Notify other = (Notify) obj;
		if(getConnectionParams() == null && other.getConnectionParams() != null) return false;
		if(getConnectionParams() != null && other.getConnectionParams() == null) return false;
		if(getConnectionParams() != null && !getConnectionParams().equals(other.getConnectionParams())) return false;
		if(getInvokeId() != other.getInvokeId()) return false;
		if(getCall() == null && other.getCall() != null) return false;
		if(getCall() != null && other.getCall() == null) return false;
		if(getCall() != null && ! getCall().equals(other.getCall())) return false;
		return true;
	}
	
}