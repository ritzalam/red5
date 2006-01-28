package org.red5.server.context;

import java.util.HashMap;
import java.util.HashSet;

public class PersistentSharedObject {

	protected String name;
	protected int version = 0;
	protected HashMap data = new HashMap();
	protected HashSet channels = new HashSet();
	
	public PersistentSharedObject(String name) {
		this.name = name;
	}
	
	public void updateAttribute(String name, Object value) {
		this.data.put(name, value);
	}
	
	public void deleteAttribute(String name) {
		this.data.remove(name);
	}
	
	public HashMap getData() {
		return this.data;
	}
	
	public int getVersion() {
		return this.version;
	}
	
	public void updateVersion() {
		this.version += 1;
	}
	
	public void registerChannel(Object channel) {
		channels.add(channel);
	}
	
	public void unregisterChannel(Object channel) {
		channels.remove(channel);
	}
	
	public HashSet getChannels() {
		return this.channels;
	}
}
