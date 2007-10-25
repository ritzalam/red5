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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import org.red5.server.api.persistence.IPersistable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread that writes modified persistent objects to the file system
 * periodically.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class FilePersistenceThread implements Runnable {

	/**
	 * Logger
	 */
	private Logger log = LoggerFactory.getLogger(FilePersistenceThread.class);

	/**
	 * Interval to serialize modified objects in milliseconds.
	 * 
	 */
	// TODO: make this configurable
	private long storeInterval = 10000;

	/**
	 * Modified objects that need to be stored.
	 */
	private Map<IPersistable, FilePersistence> modifiedObjects = new ConcurrentHashMap<IPersistable, FilePersistence>();

	/**
	 * Modified objects for each store.
	 */
	private Map<FilePersistence, Set<IPersistable>> objectStores = new ConcurrentHashMap<FilePersistence, Set<IPersistable>>();

	/**
	 * Singleton instance.
	 */
	private static volatile FilePersistenceThread instance = null;

	/**
	 * Each FilePersistenceThread has its own scheduler and the executor is
	 * guaranteed to only run a single task at a time.
	 */
	private final ScheduledExecutorService scheduler = Executors
			.newSingleThreadScheduledExecutor();

	/**
	 * Return singleton instance of the thread.
	 * 
	 * @return
	 */
	public static FilePersistenceThread getInstance() {
		return instance;
	}

	/**
	 * Create instance of the thread.
	 */
	private FilePersistenceThread() {
		super();
		if (instance != null) {
			log.error("Instance was not null, this is not a good sign");
		}
		instance = this;
		final ScheduledFuture<?> instanceHandle = scheduler
				.scheduleAtFixedRate(this, storeInterval, storeInterval,
						java.util.concurrent.TimeUnit.MILLISECONDS);
	}

	/**
	 * Notify thread that an object was modified in a persistence store.
	 * 
	 * @param object
	 * @param store
	 */
	protected void modified(IPersistable object, FilePersistence store) {
		FilePersistence previous = modifiedObjects.put(object, store);
		Set<IPersistable> objects = objectStores.get(store);
		if (objects == null) {
			objects = new HashSet<IPersistable>();
			objectStores.put(store, objects);
		}
		objects.add(object);

		if (previous != null && !previous.equals(store)) {
			log.warn("Object {} was also modified in {}, saving instantly",
					new Object[] { object, previous });
			previous.saveObject(object);
			objects = objectStores.get(previous);
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
		// Get snapshot of currently modified objects.
		Set<IPersistable> objects = objectStores.remove(store);
		if (objects != null) {
			for (IPersistable object : objects) {
				modifiedObjects.remove(object);
			}
		}

		if (objects == null || objects.isEmpty()) {
			return;
		}

		// Store pending objects
		for (IPersistable object : objects) {
			try {
				store.saveObject(object);
			} catch (Throwable e) {
				log.error("Error while saving {} in {}. {}", new Object[] {
						object, store, e });
			}
		}
	}

	/**
	 * Write modified objects to the file system periodically.
	 */
	public void run() {
		if (!modifiedObjects.isEmpty()) {
			// Get snapshot of currently modified objects.
			Map<IPersistable, FilePersistence> objects = new HashMap<IPersistable, FilePersistence>(
					modifiedObjects);
			modifiedObjects.clear();
			objectStores.clear();
			for (Map.Entry<IPersistable, FilePersistence> entry : objects
					.entrySet()) {
				try {
					entry.getValue().saveObject(entry.getKey());
				} catch (Throwable e) {
					log.error("Error while saving {} in {}. {}", new Object[] {
							entry.getKey(), entry.getValue(), e });
				}
			}
		}
	}

	/**
	 * Cleanly shutdown the tasks
	 */
	public void shutdown() {
		scheduler.shutdown();
	}
	
}
