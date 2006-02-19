package org.red5.server.api.impl;

import java.util.HashMap;

public class Session implements org.red5.server.api.Session {
	
	private HashMap attributes = new HashMap();

	public Object getAttribute(String name) {
		return attributes.get(name);
	}

	public void setAttribute(String name, Object value) {
		attributes.put(name, value);
	}

}
