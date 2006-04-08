package org.red5.server.so;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.api.so.ISharedObject;
import org.red5.server.persistence2.IPersistable;
import org.red5.server.persistence2.IPersistentStorage;
import org.red5.server.persistence2.RamPersistence;

public class ZSharedObjectService {

	protected static Log log =
        LogFactory.getLog(ZSharedObjectService.class.getName());
	
	// Persistent shared objects are configured through red5.xml
	private IPersistentStorage soPersistence = null;
	// Non-persistent shared objects are only stored in memory
	private RamPersistence soTransience = new RamPersistence(); 
	
	public ISharedObject getSharedObject(String name, boolean persistent) {
		IPersistentStorage persistence = this.soPersistence;
		if (!persistent) {
			persistence = this.soTransience;
		}
			
		if (persistence == null) {
			// XXX: maybe we should thow an exception here as a non-persistent SO doesn't make any sense...
			return null; //new SharedObject(name, false, null);
		}
		
		ISharedObject result;
		try {
			result = (ISharedObject) persistence.loadObject(SharedObject.PERSISTENT_ID_PREFIX + name);
		} catch (IOException e) {
			log.error("Could not load shared object.", e);
			result = null;
		}
		if (result == null) {
			// Create new shared object with given name
			log.info("Creating new shared object " + name);
			result = null; //new SharedObject(name, persistent, persistence);
			try {
				persistence.storeObject((IPersistable) result);
			} catch (IOException e) {
				log.error("Could not store shared object.", e);
			}
		}
		
		return result;
	}
	
}
