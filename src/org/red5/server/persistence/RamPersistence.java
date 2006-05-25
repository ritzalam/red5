package org.red5.server.persistence;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.red5.server.api.IScope;
import org.red5.server.api.persistence.IPersistable;
import org.red5.server.api.persistence.IPersistenceStore;

/**
 * Persistence implementation that stores the objects in memory.
 * This serves as default persistence if nothing has been configured.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class RamPersistence implements IPersistenceStore {

	/** This is used in the id for objects that have a name of <code>null</code> **/
	protected static final String PERSISTENCE_NO_NAME = "__null__";
	protected Map<String, IPersistable> objects = new HashMap<String, IPersistable>();
	protected IScope scope;
	
	public RamPersistence(IScope scope) {
		this.scope = scope;
	}
	
	protected String getObjectName(String id) {
		// The format of the object id is <type>/<path>/<objectName>
		String result = id.substring(id.lastIndexOf('/')+1);
		if (result.equals(PERSISTENCE_NO_NAME))
			result = null;
		return result;
	}
	
	protected String getObjectPath(String id) {
		// The format of the object id is <type>/<path>/<objectName>
		id = id.substring(id.indexOf('/')+1);
		return id.substring(0, id.lastIndexOf('/')-1);
	}
	
	protected String getObjectId(IPersistable object) {
		// The format of the object id is <type>/<path>/<objectName>
		String result = object.getType() + "/" + object.getPath();
		if (!result.endsWith("/"))
			result += "/";
		String name = object.getName();
		if (name == null)
			name = PERSISTENCE_NO_NAME;
		return result + name;
	}
	
	public synchronized boolean save(IPersistable object) {
		objects.put(getObjectId(object), object);
		object.setPersistent(true);
		return true;
	}
	
	public synchronized IPersistable load(String name) {
		return objects.get(name);
	}
	
	public boolean load(IPersistable obj) {
		return obj.isPersistent();
	}
	
	public synchronized boolean remove(IPersistable object) {
		return remove(getObjectId(object));
	}
	
	public synchronized boolean remove(String name) {
		if (!objects.containsKey(name))
			return false;
		
		IPersistable object = objects.remove(name);
		object.setPersistent(false);
		return true;
	}
	
	public Iterator<String> getObjectNames() {
		return objects.keySet().iterator();
	}
	
	public Iterator<IPersistable> getObjects() {
		return objects.values().iterator();
	}
}
