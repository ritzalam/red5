package org.red5.server;

import java.util.Map;

import org.red5.server.api.persistence.IPersistable;

public class PersistableAttributeStore extends AttributeStore 
	implements IPersistable {
	
	protected boolean persistent = true;
	protected String name;
	protected String type;
	protected String path;
	protected long lastModified = -1;
	
	public PersistableAttributeStore(String type, String name, String path, boolean persistent){
		this.type = type;
		this.name = name;
		this.path = path;
		this.persistent = persistent;
	}
	
	protected void modified(){
		lastModified = System.currentTimeMillis();
	}
	
	public boolean isPersistent() {
		return persistent;
	}

	public long getLastModified() {
		return lastModified;
	}

	public String getName() {
		return name;
	}

	public String getPath() {
		return path;
	}

	public String getType() {
		return type;
	}

	public Object convertToSerialzable() {
		return attributes;
	}
	
	public boolean loadFromSerializable(Object from) {
		if(from instanceof Map){
			Map map = (Map) from;
			attributes.clear();
			attributes.putAll(map);
			return true;
		} else return false;
	}

}
