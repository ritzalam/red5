/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2012 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.server.persistence;

import java.util.Map;
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
	 * Modified objects.
	 */
	private Map<UpdateEntry, FilePersistence> objects = new ConcurrentHashMap<UpdateEntry, FilePersistence>();

	/**
	 * Singleton instance.
	 */
	private static volatile FilePersistenceThread instance = null;

	/**
	 * Each FilePersistenceThread has its own scheduler and the executor is
	 * guaranteed to only run a single task at a time.
	 */
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	/**
	 * Return singleton instance of the thread.
	 * 
	 * @return singleton instance of thread.
	 */
	public static FilePersistenceThread getInstance() {
		return instance;
	}

	/**
	 * Create instance of the thread.
	 */
	private FilePersistenceThread() {
		if (instance != null) {
			log.error("Instance was not null, this is not a good sign");
		}
		instance = this;
		@SuppressWarnings("unused")
		final ScheduledFuture<?> instanceHandle = scheduler.scheduleAtFixedRate(this, storeInterval, storeInterval, java.util.concurrent.TimeUnit.MILLISECONDS);
	}

	/**
	 * Notify thread that an object was modified in a persistence store.
	 * 
	 * @param object
	 * @param store
	 */
	protected void modified(IPersistable object, FilePersistence store) {
		FilePersistence previous = objects.put(new UpdateEntry(object, store), store);
		if (previous != null && !previous.equals(store)) {
			log.warn("Object {} was also modified in {}, saving instantly", new Object[] { object, previous });
			previous.saveObject(object);
		}
	}

	/**
	 * Write any pending objects for the given store to disk.
	 * 
	 * @param store
	 */
	protected void notifyClose(FilePersistence store) {
		// Store pending objects for this store
		for (UpdateEntry entry : objects.keySet()) {
			if (!store.equals(entry.store)) {
				// Object is from different store
				continue;
			}
			try {
				objects.remove(entry);
				store.saveObject(entry.object);
			} catch (Throwable e) {
				log.error("Error while saving {} in {}. {}", new Object[] { entry.object, store, e });
			}
		}
	}

	/**
	 * Write modified objects to the file system periodically.
	 */
	public void run() {
		if (!objects.isEmpty()) {
			for (UpdateEntry entry : objects.keySet()) {
				try {
					objects.remove(entry);
					entry.store.saveObject(entry.object);
				} catch (Throwable e) {
					log.error("Error while saving {} in {}. {}", new Object[] { entry.object, entry.store, e });
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

	/**
	 * Informations about one entry to object.
	 */
	private static class UpdateEntry {

		/** Object to store. */
		IPersistable object;

		/** Store the object should be serialized to. */
		FilePersistence store;

		/**
		 * Create new update entry.
		 * 
		 * @param object	object to serialize
		 * @param store		store the object should be serialized in
		 */
		UpdateEntry(IPersistable object, FilePersistence store) {
			this.object = object;
			this.store = store;
		}

		/**
		 * Compare with another entry.
		 * 
		 * @param other		entry to compare to
		 * @return <code>true</code> if entries match, otherwise <code>false</code>
		 */
		@Override
		public boolean equals(Object other) {
			if (!(other instanceof UpdateEntry)) {
				return false;
			}

			return (object.equals(((UpdateEntry) other).object) && store.equals(((UpdateEntry) other).store));
		}

		/**
		 * Return hash value for entry.
		 * 
		 * @return the hash value
		 */
		@Override
		public int hashCode() {
			return object.hashCode() + store.hashCode();
		}

	}

}
