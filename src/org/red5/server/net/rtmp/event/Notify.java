package org.red5.server.net.rtmp.event;

import java.util.Map;

import org.apache.mina.common.ByteBuffer;
import org.red5.server.api.service.IServiceCall;
import org.red5.server.stream.IStreamData;

public class Notify extends BaseEvent implements IStreamData {
	
	protected IServiceCall call = null;
	protected ByteBuffer data = null;
	private int invokeId = 0;
	private Map connectionParams = null;
	
	public Notify() {
		super(Type.SERVICE_CALL);
	}

	public Notify(ByteBuffer data){
		super(Type.STREAM_DATA);
		this.data = data;
	}
	
	public Notify(IServiceCall call){
		super(Type.SERVICE_CALL);
		this.call = call;
	}
	
	public byte getDataType() {
		return TYPE_NOTIFY;
	}
	
	public void setCall(IServiceCall call) {
		this.call = call;
	}
	
	public IServiceCall getCall() {
		return this.call;
	}
	
	public ByteBuffer getData() {
		return data;
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
	
	public void release() {
		if (data != null) {
			data.release();
			data = null;
		}
		super.release();
	}
}