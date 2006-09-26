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
import org.apache.commons.pool.PoolableObjectFactory;

/**
 * @author Murali Kosaraju ThreadObjectFactory - Extends PoolableObjectFactory
 *         and overrides some lifecycle methods to exercise control over how the
 *         resources(threads) are managed.
 */
public class ThreadObjectFactory implements PoolableObjectFactory {
	/**
	 * Logger for this class
	 */
	private static final Log log = LogFactory.getLog(ThreadObjectFactory.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.commons.pool.PoolableObjectFactory#makeObject()
	 */
	public Object makeObject() {
		log.debug(" makeObject...");
		return new WorkerThread();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.commons.pool.PoolableObjectFactory#destroyObject(java.lang.Object)
	 */
	public void destroyObject(Object obj) {
		log.debug(" !!! destroyObject... !!!" + obj);
		if (obj instanceof WorkerThread) {
			WorkerThread rt = (WorkerThread) obj;
			rt.setStopped(true);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.commons.pool.PoolableObjectFactory#validateObject(java.lang.Object)
	 */
	public boolean validateObject(Object obj) {
		log.debug(" validateObject..." + obj);
		if (obj instanceof WorkerThread) {
			WorkerThread rt = (WorkerThread) obj;
			if (!rt.isDone()) { // if the thread is running the previous task,
				// get another one.
				return false;
			}
			if (rt.isRunning()) {
				if (rt.getThreadGroup() == null) {
					return false;
				}
				return true;
			}
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.commons.pool.PoolableObjectFactory#activateObject(java.lang.Object)
	 */
	public void activateObject(Object obj) {
		log.debug(" activateObject...");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.commons.pool.PoolableObjectFactory#passivateObject(java.lang.Object)
	 */
	public void passivateObject(Object obj) {
		log.debug(" passivateObject..." + obj);
		if (obj instanceof WorkerThread) {
			WorkerThread wt = (WorkerThread) obj;
			wt.setResult(null);
		}
	}

}