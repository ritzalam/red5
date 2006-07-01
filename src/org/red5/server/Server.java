package org.red5.server;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006 by respective authors (see below). All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either version 2.1 of the License, or (at your option) any later 
 * version. 
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this library; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 */

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.api.IGlobalScope;
import org.red5.server.api.IServer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.style.ToStringCreator;

public class Server implements IServer, ApplicationContextAware {

	// Initialize Logging
	protected static Log log =
        LogFactory.getLog(Server.class.getName());
	
	protected HashMap<String,IGlobalScope> globals = new HashMap<String,IGlobalScope>();
	protected HashMap<String, String> mapping = new HashMap<String,String>();
	protected ApplicationContext applicationContext;
	private static final String SLASH = "/";
	private static final String EMPTY = "";
	
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}
	
	protected String getKey(String hostName, String contextPath){
		if(hostName==null) hostName = EMPTY;
		if(contextPath==null) contextPath = EMPTY;
		return hostName +SLASH+ contextPath;
	}

	public IGlobalScope lookupGlobal(String hostName, String contextPath){
		String key = getKey(hostName, contextPath);
		while(contextPath.indexOf(SLASH) != -1){
			key = getKey(hostName, contextPath);
			log.debug("Check: "+key);
			if(mapping.containsKey(key)) return getGlobal(mapping.get(key));
			contextPath = contextPath.substring(0,contextPath.lastIndexOf(SLASH));
		}
		key = getKey(hostName, contextPath);
		log.debug("Check host and path: "+key);
		if(mapping.containsKey(key)) return getGlobal(mapping.get(key));
		key = getKey(EMPTY, contextPath);
		log.debug("Check wildcard host with path: "+key);
		if(mapping.containsKey(key)) return getGlobal(mapping.get(key));
		key = getKey(hostName, EMPTY);
		log.debug("Check host with no path: "+key);
		if(mapping.containsKey(key)) return getGlobal(mapping.get(key));
		key = getKey(EMPTY, EMPTY);
		log.debug("Check default host, default path: "+key);
		return getGlobal(mapping.get(key));
	}
	
	public IGlobalScope getGlobal(String name) {
		return globals.get(name);
	}

	public void registerGlobal(IGlobalScope scope) {
		log.info("Registering global scope: "+scope.getName());
		globals.put(scope.getName(),scope);
	}

	public boolean addMapping(String hostName, String contextPath, String globalName){
		final String key = getKey(hostName, contextPath);
		log.debug("Add mapping: "+key+" => "+globalName);
		if(mapping.containsKey(key)) return false;
		mapping.put(key, globalName);
		return true;
	}
	
	public boolean removeMapping(String hostName, String contextPath){
		final String key = getKey(hostName, contextPath);
		log.debug("Remove mapping: "+key);
		if(!mapping.containsKey(key)) return false;
		mapping.remove(key);
		return true;
	}
	
	public Map<String,String> getMappingTable(){
		return mapping;
	}

	public Iterator<String> getGlobalNames() {
		return globals.keySet().iterator();
	}

	public Iterator<IGlobalScope> getGlobalScopes() {
		return globals.values().iterator();
	}
	
	public String toString(){
		return new ToStringCreator(this).append(mapping).toString();
	}
	

}