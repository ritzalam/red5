package org.red5.server.webapp.oflaDemo;

import java.util.HashMap;
import java.util.Map;

public class DemoServiceImpl implements IDemoService {
	/**
     * Getter for property 'listOfAvailableFLVs'.
     *
     * @return Value for property 'listOfAvailableFLVs'.
     */
    public Map getListOfAvailableFLVs() {
		return new HashMap(1);
	}
    
    public Map getListOfAvailableFLVs(String string) {
    	System.out.println("Got a string: " + string);
    	return getListOfAvailableFLVs();
    }
    
}

