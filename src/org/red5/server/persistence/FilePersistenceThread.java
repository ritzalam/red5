package org.red5.server.persistence;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 *
 * Copyright (c) 2006-2007 by respective authors (see below). All rights reserved.
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.api.persistence.IPersistable;

/**
 * Thread that writes modified persistent objects to the filesystem periodically.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class FilePersistenceThread extends Thread {

    /**
     * Logger
     */
    private Log log = LogFactory.getLog(FilePersistenceThread.class.getName());
    
	/**
	 * Interval to serialize modified objects in milliseconds.
	 *
	 */
    // TODO: make this configurable
	private int storeInterval = 10000;
	
	/**
	 * Modified objects that need to be stored.
	 */
	private Map<IPersistable, FilePersistence> modifiedObjects = new HashMap<IPersistable, FilePersistence>();
	
	/**
	 * Modified objects for each store.
	 */
	private Map<FilePersistence, Set<IPersistable>> objectStores = new HashMap<FilePersistence, Set<IPersistable>>();
	
	/**
	 * Singleton instance.
	 */
	private static volatile FilePersistenceThread instance = null;
	
	/**
	 * Return singleton instance of the thread.
	 * 
	 * @return
	 */
	public static FilePersistenceThread getInstance() {
		if (instance == null) {
			// Only synchronize if thread doesn't exist yet.
			synchronized (FilePersistenceThread.class) {
				if (instance == null) {
					instance = new FilePersistenceThread();
					instance.start();
				}
			}
		}
		
		return instance;
	}
	
	/**
	 * Create instance of the thread.
	 */
	private FilePersistenceThread() {
		super();
		setName("FilePersistenceThread");
	}

	/**
	 * Notify thread that an object was modified in a persistence store.
	 * 
	 * @param object
	 * @param store
	 */
	protected void modified(IPersistable object, FilePersistence store) {
		FilePersistence previous;
		synchronized (modifiedObjects) {
			previous = modifiedObjects.put(object, store);
			Set<IPersistable> objects = objectStores.get(store);
			if (objects == null) {
				objects = new HashSet<IPersistable>();
				objectStores.put(store, objects);
			}
			objects.add(object);
		}
		
		if (previous != null && !previous.equals(store)) {
			log.warn("Object " + object + " was also modified in " + previous + ", saving instantly");
			previous.saveObject(object);
			Set<IPersistable> objects = objectStores.get(previous);
			if (objects != null) {
				objects.remove(previous);
			}
		}
	}
	
	/**
	 * Write any pending objects for the given store to disk.
	 * 
	 * @param store
	 */
	protected void notifyClose(FilePersistence store) {
		Set<IPersistable> objects;
		// Get snapshot of currently modified objects.
		synchronized (modifiedObjects) {
			objects = objectStores.remove(store);
			if (objects != null) {
				for (IPersistable object: objects) {
					modifiedObjects.remove(object);
				}
			}
		}
		
		if (objects == null || objects.isEmpty()) {
			return;
		}
		
		// Store pending objects
		for (IPersistable object: objects) {
			try {
				store.saveObject(object);
			} catch (Throwable e) {
				log.error("Error while saving " + object + " in " + store, e);
			}
		}
	}
	
    /**
     * Write modified objects to the filesystem periodically.
     */
	public void run() {
		while (isAlive()) {
			long start = System.currentTimeMillis();
			if (!modifiedObjects.isEmpty()) {
				Map<IPersistable, FilePersistence> objects;
				// Get snapshot of currently modified objects.
				synchronized (modifiedObjects) {
					objects = new HashMap<IPersistable, FilePersistence>(modifiedObjects);
					modifiedObjects.clear();
					objectStores.clear();
				}
				
				for (Map.Entry<IPersistable, FilePersistence> entry: objects.entrySet()) {
					try {
						entry.getValue().saveObject(entry.getKey());
					} catch (Throwable e) {
						log.error("Error while saving " + entry.getKey() + " in " + entry.getValue(), e);
					}
				}
			}
			long end = System.currentTimeMillis();
			try {
				long delay = storeInterval - (end - start);
				if (delay > 0) {
					Thread.sleep(delay);
				}
			} catch (InterruptedException e) {
				// Ignore
			}
		}
	}

}
