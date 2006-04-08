package org.red5.server.api;

import java.util.Iterator;
import java.util.Map;

public interface IServer {

	public static final String ID = "red5.server";
	
	public IGlobalScope getGlobal(String name);
	public void registerGlobal(IGlobalScope scope);
	public IGlobalScope lookupGlobal(String hostName, String contextPath);
	public boolean addMapping(String hostName, String contextPath, String globalName);
	public boolean removeMapping(String hostName, String contextPath);
	public Map<String,String> getMappingTable();		
	public Iterator<String> getGlobalNames();
	public Iterator<IGlobalScope> getGlobalScopes();
	
}