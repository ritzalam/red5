package org.red5.server.persistence2;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Persistence implementation that stores the objects in memory.
 * This serves as default persistence if nothing has been configured.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class RamPersistence implements IPersistentStorage {

	private HashMap objects = new HashMap();
	private final static String GENERATED_ID_PREFIX = "_RED5_GENERATED_"; 
	private int objectId = 0;
	
	public synchronized String newPersistentId() {
		objectId++;
		return GENERATED_ID_PREFIX + new Integer(objectId-1).toString();
	}
	
	public synchronized void storeObject(IPersistable object) throws IOException {
		objects.put(object.getPersistentId(), object);
	}
	
	public synchronized IPersistable loadObject(String name) throws IOException {
		return (IPersistable) objects.get(name);
	}
	
	public synchronized void removeObject(String name) throws IOException {
		objects.remove(name);
	}
	
	public Iterator getObjects() {
		return objects.values().iterator();
	}
}
