package org.red5.server.webapp.test;

import org.red5.server.adapter.ApplicationAdapter;

public class Application extends ApplicationAdapter {
	
	public String test(String val){
		log.debug("test called on: "+getName());
		return val + val;
	}

}
