package org.red5.server;

import java.io.IOException;
import java.util.Map;
import org.red5.io.object.Deserializer;
import org.red5.io.object.Input;
import org.red5.io.object.Output;
import org.red5.io.object.Serializer;
import org.red5.server.api.persistence.IPersistable;
import org.red5.server.api.persistence.IPersistenceStore;

public class PersistableAttributeStore extends AttributeStore 
	implements IPersistable {
	
	protected boolean persistent = true;
	protected String name;
	protected String type;
	protected String path;
	protected long lastModified = -1;
	protected IPersistenceStore store = null;
	
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

	public void setPersistent(boolean persistent) {
		this.persistent = persistent;
	}

	public long getLastModified() {
		return lastModified;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
	
	public String getType() {
		return type;
	}

	public void serialize(Output output) throws IOException {
		Serializer serializer = new Serializer();
		serializer.serialize(output, attributes);
	}

	public void deserialize(Input input) throws IOException {
		Deserializer deserializer = new Deserializer();
		Object obj = deserializer.deserialize(input);
		if (!(obj instanceof Map))
			throw new IOException("required Map object");
		
		attributes.clear();
		attributes.putAll((Map<String, Object>) obj);
	}

	public void setStore(IPersistenceStore store) {
		this.store = store;
	}
	
}
