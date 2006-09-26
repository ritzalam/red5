/*
 * Copyright 2004-2005 the original author.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.red5.server.pooling;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

/**
 * ThreadPool - Extends GenericObjectPool. Overrides some methods and provides
 * some helper methods.
 * 
 * @author Murali Kosaraju
 */
public class ThreadPool extends GenericObjectPool {
	/**
	 * Logger for this class
	 */
	private static final Log log = LogFactory.getLog(ThreadPool.class);

	/**
	 * Constructor when there is no configuration available. Please refer
	 * commons-pooling for more details on the configuration parameters.
	 * 
	 * @param objFactory -
	 *            The factory to be used for thread creation.
	 */
	public ThreadPool(ThreadObjectFactory objFactory) {
		super(objFactory);
		this.setWhenExhaustedAction((byte) 2);
		this.setMaxIdle(2); // maximum idle threads
		this.setMaxActive(4); // maximum active threads.
		this.setMinEvictableIdleTimeMillis(30000); // Evictor runs every 30
		// secs.
		this.setTestOnBorrow(true); // check if the thread is still valid
		// this.setMaxWait(1000); // 1 second wait when threads are not
		// available.
	}

	/**
	 * Constructor to be used when there is a configuration available.
	 * 
	 * @param objFactory -
	 *            The factory to be used for thread creation..
	 * @param config -
	 *            This could be created by loading a properties file.
	 */
	public ThreadPool(ThreadObjectFactory objFactory,
			GenericObjectPool.Config config) {
		super(objFactory, config);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.commons.pool.ObjectPool#borrowObject()
	 */
	@Override
	public Object borrowObject() throws Exception {
		log.debug(" borrowing object..");
		return super.borrowObject();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.commons.pool.ObjectPool#returnObject(java.lang.Object)
	 */
	@Override
	public void returnObject(Object obj) throws Exception {
		log.debug(" returning object.." + obj);
		super.returnObject(obj);
	}

	/**
	 * borrowObjects - Helper method, use this carefully since this could be a
	 * blocking call when the threads requested may not be avialable based on
	 * the configuration being used.
	 * 
	 * @param num
	 * @return WorkerThread[]
	 */
	public synchronized WorkerThread[] borrowObjects(int num) {
		WorkerThread[] rtArr = new WorkerThread[num];
		for (int i = 0; i < num; i++) {
			WorkerThread rt;
			try {
				rt = (WorkerThread) borrowObject();
				rtArr[i] = rt;
			} catch (Exception e) {
				log.error(" borrowObjects failed.. ", e);
			}
		}
		return rtArr;
	}

}