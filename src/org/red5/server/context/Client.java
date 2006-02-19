package org.red5.server.context;

import java.util.Map;

public class Client {

	protected Map params = null;
	
	public String getParameter(String name){
		if(params == null) return null;
		else return (String) params.get(name);
	}
	
	public void close(){
		// nothing
	}
	
}
