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

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Murali Kosaraju WorkerThread class - This is the worker thread which
 *         gets pooled for re-use. We should customize this for the desired
 *         behaviour.
 */
public class WorkerThread extends Thread implements WorkerThreadMBean {

	private static Logger log = LoggerFactory.getLogger(WorkerThread.class);

	/** Constructs a new WorkerThread. */
    public WorkerThread() {
		// default constructor
	}

	/**
	 * Keeps the thread running when false. When set to true, completes the
	 * execution of the thread and stops.
	 */
	private boolean stopped;

	/**
	 * Manages the thread's state. When the thread is first created, running is
	 * set to false. When the thread is assigned a task for the first time, the
	 * thread continues to be in the running state until stopped.
	 */
	private boolean running;

	/**
	 * Manages the thread's internal state with respect to the task. When the
	 * thread finishes executing the task, this is set to true.
	 */
	private boolean done = true;

	/**
	 * The class name where the method to be execute is defined.
	 */
	private String className;

	/**
	 * The method name to be executed.
	 */
	private String methodName;

	/**
	 * The parameters to be passed for the method.
	 */
	private Object[] methodParams;

	/**
	 * The parameter types for the respective parameters..
	 */
	private Class<?>[] parmTypes;

	/**
	 * The object to synchronize upon for notifying the completion of task.
	 */
	private Object syncObject;

	/**
	 * The result of our execution.
	 */
	private Object result;

	/**
	 * The pool being used. We use this if we need to return the object back to
	 * the pool. If this is not set, we assume that the client will take care of
	 * returning the object back to the pool.
	 */
	private ThreadPool pool;

	/**
	 * @param pool
	 *            The pool to set.
	 */
	public void setPool(ThreadPool pool) {
		this.pool = pool;
	}

	/**
	 * @param result
	 *            The result to set.
	 */
	public void setResult(Object result) {
		this.result = result;
	}

	/**
	 * @return Returns the result.
	 */
	public Object getResult() {
		return result;
	}

	/**
	 * @return Returns the className.
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * @param className
	 *            The className to set.
	 */
	public void setClassName(String className) {
		this.className = className;
	}

	/**
	 * @return Returns the done.
	 */
	public boolean isDone() {
		return done;
	}

	/**
	 * @param done
	 *            The done to set.
	 */
	public void setDone(boolean done) {
		this.done = done;
	}

	/**
	 * @return Returns the methodName.
	 */
	public String getMethodName() {
		return methodName;
	}

	/**
	 * @param methodName
	 *            The methodName to set.
	 */
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	/**
	 * @return Returns the methodParams.
	 */
	public Object[] getMethodParams() {
		return methodParams;
	}

	/**
	 * @param methodParams
	 *            The methodParams to set.
	 */
	public void setMethodParams(Object[] methodParams) {
		this.methodParams = methodParams;
	}

	/**
	 * @return Returns the parmTypes.
	 */
	public Class<?>[] getParmTypes() {
		return parmTypes;
	}

	/**
	 * @param parmTypes
	 *            The parmTypes to set.
	 */
	public void setParmTypes(Class<?>[] parmTypes) {
		this.parmTypes = parmTypes;
	}

	/**
	 * @return Returns the running.
	 */
	public boolean isRunning() {
		return running;
	}

	/**
	 * @param running
	 *            The running to set.
	 */
	public void setRunning(boolean running) {
		this.running = running;
	}

	/**
	 * @return Returns the stopped.
	 */
	public boolean isStopped() {
		return stopped;
	}

	/**
	 * @param stopped
	 *            The stopped to set.
	 */
	public void setStopped(boolean stopped) {
		this.stopped = stopped;
	}

	/**
	 * @return Returns the syncObject.
	 */
	public Object getSyncObject() {
		return syncObject;
	}

	/**
	 * @param syncObject
	 *            The syncObject to set.
	 */
	public void setSyncObject(Object syncObject) {
		this.syncObject = syncObject;
	}

	/**
	 * execute
	 * 
	 * @param clsName
	 * @param methName
	 * @param params
     * @param paramTypes
	 * @param synObj
     * @param paramTypes
	 */
	public synchronized void execute(String clsName, String methName,
			Object[] params, Class<?>[] paramTypes, Object synObj) {
		this.className = clsName;
		this.methodName = methName;
		this.methodParams = params;
		this.syncObject = synObj;
		this.parmTypes = paramTypes;
		this.done = false;

		if (!running) { // If this is the first time, then kick off the thread.
			this.setDaemon(true);
			this.start();
		} else { // we already have a thread running so wakeup the waiting
			// thread.
			this.notifyAll();
		}
	}

	/** {@inheritDoc} */
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		running = true;
		while (!stopped) {
			if (done) {
				synchronized (this) {
					try {
						this.wait();
					} catch (InterruptedException e) {
						stopped = true;
						log.error("", e);
					}
				}
			} else { // there is a task....let us execute it.
				try {
					execute();
				} catch (Exception e) {
					log.error("", e);
				} finally {
					if (syncObject != null) {
						synchronized (syncObject) {
							syncObject.notify();
						}
					}
					reset();
					returnToPool();
				}
			}
		}
	}

	/**
	 * For asynchronous execution, without waiting, the pool should be set, so
	 * that we can return the thread back to the pool. No need to use any
	 * <code>wait()</code> or <code>notify()</code> methods. see :
	 * <code>runAsyncTask()</code> method in PoolTester class.
	 * 
	 * <pre>
	 *   Example : 
	 *           ThreadPool pool = new ThreadPool(new ThreadObjectFactory());
	 *           WorkerThread wt = (WorkerThread) pool.borrowObject();
	 *           wt.setPool(pool);       wt.execute(.....) ;
	 * </pre>
	 */
	private void returnToPool() {
		if (pool != null) {
			try {
				pool.returnObject(this);
			} catch (Exception e1) {
				log.error("Exception :", e1);
			}
			this.pool = null;
		}
	}

	/**
	 * reset the memebers to service next request.
	 */
	public void reset() {
		this.done = true;
		this.className = null;
		this.methodName = null;
		this.methodParams = null;
		this.parmTypes = null;
		this.syncObject = null;
	}

	/**
	 * getClass
	 * 
	 * @param cls
	 * @return Class
	 * @throws ClassNotFoundException
	 */
	private static Class<?> getClass(String cls) throws ClassNotFoundException {
		ClassLoader classLoader = Thread.currentThread()
				.getContextClassLoader();
		if (classLoader == null) {
			classLoader = WorkerThread.class.getClassLoader();
		}
		return classLoader.loadClass(cls);
	}

	/**
	 * execute
	 */
	private void execute() {
		try {
			Class<?> cls = getClass(this.getClassName());
			Object obj = cls.newInstance();
			this.result = MethodUtils.invokeExactMethod(obj, this
					.getMethodName(), this.getMethodParams(), this
					.getParmTypes());
			if (log.isDebugEnabled()) {
				log.debug(" #### Execution Result = " + result + " for : " + this);
			}
		} catch (ClassNotFoundException e) {
			log.error("ClassNotFoundException - " + e);
		} catch (NoSuchMethodException e) {
			log.error("NoSuchMethodException - " + e);
		} catch (IllegalAccessException e) {
			log.error("IllegalAccessException - " + e);
		} catch (InvocationTargetException e) {
			log.error("InvocationTargetException - " + e);
		} catch (InstantiationException e) {
			log.error("InstantiationException - " + e);
		}
	}

}
