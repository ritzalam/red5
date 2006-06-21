package org.red5.server.net.rtmp_refactor.status;


public class Status implements StatusCodes {

	public static final String ERROR = "error";
	public static final String STATUS = "status";
	public static final String WARNING = "warning";
	
	protected String code;
	protected String level;
	protected String description = "";
	protected String details = "";
	protected int clientid = 0;
	
	public Status(){
		
	}
	
	public Status(String code){
		this.code = code;
		this.level = STATUS;
	}
	
	public Status(String code, String level, String description){
		this.code = code;
		this.level = level;
		this.description = description;
	}
	
	public String getCode() {
		return code;
	}
	
	public void setCode(String code) {
		this.code = code;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDesciption(String description) {
		this.description = description;
	}
	
	public String getLevel(){
		return level;
	}
	
	public void setLevel(String level) {
		this.level = level;
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



	public void setDescription(String description) {
		this.description = description;
	}



	public String toString(){
		return "Status: "+code;
	}
	
}
