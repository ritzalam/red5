package org.red5.server.net.rtmp.status;

public class RuntimeStatusObject extends StatusObject {

	protected String details = "";
	protected int clientid = 0;
	
	public RuntimeStatusObject(){
		super();
	}
	
	public RuntimeStatusObject(String code, String level, String description){
		super(code, level, description);
	}
	
	public RuntimeStatusObject(String code, String level, String description, 
			String details, int clientid){
		super(code, level, description);
		this.details = details;
		this.clientid = clientid;
	}
	
	public int getClientid() {
		return clientid;
	}
	
	public void setClientid(int clientid) {
		this.clientid = clientid;
	}
	
	public String getDetails() {
		return details;
	}
	
	public void setDetails(String details) {
		this.details = details;
	}
	
}
