package org.red5.server.so;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006 by respective authors (see below). All rights reserved.
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

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.BaseConnection;
import org.red5.server.BasicScope;
import org.red5.server.api.IAttributeStore;
import org.red5.server.api.IContext;
import org.red5.server.api.IScope;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.event.IEventListener;
import org.red5.server.api.persistence.IPersistenceStore;
import org.red5.server.api.so.ISharedObject;
import org.red5.server.api.so.ISharedObjectListener;
import org.red5.server.service.ServiceUtils;

public class SharedObjectScope extends BasicScope implements ISharedObject {

	private Log log = LogFactory.getLog(SharedObjectScope.class.getName());

	private final ReentrantLock lock = new ReentrantLock();

	private HashSet<ISharedObjectListener> serverListeners = new HashSet<ISharedObjectListener>();

	private HashMap<String, Object> handlers = new HashMap<String, Object>();

	protected SharedObject so;

	public SharedObjectScope(IScope parent, String name, boolean persistent,
			IPersistenceStore store) {
		super(parent, TYPE, name, persistent);

		// Create shared object wrapper around the attributes
		String path = parent.getContextPath();
		if ("".equals(path) || path.charAt(0) != '/') {
			path = '/' + path;
		}
		so = (SharedObject) store.load(TYPE + path + '/' + name);
		if (so == null) {
			so = new SharedObject(attributes, name, path, persistent, store);

			store.save(so);
		} else {
			so.setName(name);
			so.setPath(parent.getContextPath());
		}
	}

	public void setPersistenceClass(String persistenceClass) {
		// Nothing to do here, the shared object will take care of persistence.
	}

	@Override
	public IPersistenceStore getStore() {
		return so.getStore();
	}

	@Override
	public String getName() {
		return so.getName();
	}

	@Override
	public void setName(String name) {
		so.setName(name);
	}

	@Override
	public String getPath() {
		return so.getPath();
	}

	@Override
	public void setPath(String path) {
		so.setPath(path);
	}

	@Override
	public String getType() {
		return so.getType();
	}

	public boolean isPersistentObject() {
		return so.isPersistentObject();
	}

	public synchronized void beginUpdate() {
		if (!lock.isHeldByCurrentThread()) {
			lock.lock();
		}
		so.beginUpdate();
	}

	public synchronized void beginUpdate(IEventListener listener) {
		if (!lock.isHeldByCurrentThread()) {
			lock.lock();
		}
		so.beginUpdate(listener);
	}

	public synchronized void endUpdate() {
		so.endUpdate();
		if (so.updateCounter == 0) {
			lock.unlock();
		}
	}

	public int getVersion() {
		return so.getVersion();
	}

	public void sendMessage(String handler, List arguments) {
		beginUpdate();
		so.sendMessage(handler, arguments);
		endUpdate();

		// Invoke method on registered handler
		String serviceName, serviceMethod;
		int dotPos = handler.lastIndexOf('.');
		if (dotPos != -1) {
			serviceName = handler.substring(0, dotPos);
			serviceMethod = handler.substring(dotPos + 1);
		} else {
			serviceName = "";
			serviceMethod = handler;
		}

		Object soHandler = getServiceHandler(serviceName);
		if (soHandler == null && hasParent()) {
			// No custom handler, check for service defined in the scope's
			// context
			IContext context = getParent().getContext();
			try {
				// The bean must have a name of
				// "<SharedObjectName>.<DottedServiceName>.soservice"
				soHandler = context.getBean(so.getName() + '.' + serviceName
						+ ".soservice");
			} catch (Exception err) {
				log.debug("No such bean");
			}
		}

		if (soHandler != null) {
			Object[] methodResult = ServiceUtils.findMethodWithExactParameters(
					soHandler, serviceMethod, arguments);
			if (methodResult.length == 0 || methodResult[0] == null) {
				methodResult = ServiceUtils.findMethodWithListParameters(
						soHandler, serviceMethod, arguments);
			}

			if (methodResult.length > 0 && methodResult[0] != null) {
				Method method = (Method) methodResult[0];
				Object[] params = (Object[]) methodResult[1];
				try {
					method.invoke(soHandler, params);
				} catch (Exception err) {
					log.error("Error while invoking method " + serviceMethod
							+ " on shared object handler " + handler, err);
				}
			}
		}

		// Notify server listeners
		Iterator<ISharedObjectListener> it = serverListeners.iterator();
		while (it.hasNext()) {
			ISharedObjectListener listener = it.next();
			listener.onSharedObjectSend(this, handler, arguments);
		}
	}

	@Override
	public synchronized boolean removeAttribute(String name) {
		beginUpdate();
		boolean success = so.removeAttribute(name);
		endUpdate();

		if (success) {
			Iterator<ISharedObjectListener> it = serverListeners.iterator();
			while (it.hasNext()) {
				ISharedObjectListener listener = it.next();
				listener.onSharedObjectDelete(this, name);
			}
		}
		return success;
	}

	@Override
	public synchronized void removeAttributes() {
		beginUpdate();
		so.removeAttributes();
		endUpdate();

		Iterator<ISharedObjectListener> it = serverListeners.iterator();
		while (it.hasNext()) {
			ISharedObjectListener listener = it.next();
			listener.onSharedObjectClear(this);
		}
	}

	@Override
	public void addEventListener(IEventListener listener) {
		super.addEventListener(listener);
		so.register(listener);

		Iterator<ISharedObjectListener> it = serverListeners.iterator();
		while (it.hasNext()) {
			ISharedObjectListener soListener = it.next();
			soListener.onSharedObjectConnect(this);
		}
	}

	@Override
	public void removeEventListener(IEventListener listener) {
		so.unregister(listener);
		super.removeEventListener(listener);
		if (!so.isPersistentObject() && so.getListeners().isEmpty()) {
			getParent().removeChildScope(this);
		}

		Iterator<ISharedObjectListener> it = serverListeners.iterator();
		while (it.hasNext()) {
			ISharedObjectListener soListener = it.next();
			soListener.onSharedObjectDisconnect(this);
		}
	}

	@Override
	public boolean hasAttribute(String name) {
		return so.hasAttribute(name);
	}

	@Override
	public Object getAttribute(String name) {
		return so.getAttribute(name);
	}

	@Override
	public Set<String> getAttributeNames() {
		return so.getAttributeNames();
	}

	public Map<String, Object> getData() {
		return so.getData();
	}

	@Override
	public void dispatchEvent(IEvent e) {
		if (e.getType() != IEvent.Type.SHARED_OBJECT
				|| !(e instanceof ISharedObjectMessage)) {
			// Don't know how to handle this event.
			super.dispatchEvent(e);
			return;
		}

		ISharedObjectMessage msg = (ISharedObjectMessage) e;
		if (msg.hasSource()) {
			beginUpdate(msg.getSource());
		} else {
			beginUpdate();
		}
		for (ISharedObjectEvent event : msg.getEvents()) {
			switch (event.getType()) {
				case SERVER_CONNECT:
					if (msg.hasSource()) {
						IEventListener source = msg.getSource();
						if (source instanceof BaseConnection) {
							((BaseConnection) source).registerBasicScope(this);
						} else {
							addEventListener(source);
						}
					}
					break;
				case SERVER_DISCONNECT:
					if (msg.hasSource()) {
						IEventListener source = msg.getSource();
						if (source instanceof BaseConnection) {
							((BaseConnection) source)
									.unregisterBasicScope(this);
						} else {
							removeEventListener(source);
						}
					}
					break;
				case SERVER_SET_ATTRIBUTE:
					setAttribute(event.getKey(), event.getValue());
					break;
				case SERVER_DELETE_ATTRIBUTE:
					removeAttribute(event.getKey());
					break;
				case SERVER_SEND_MESSAGE:
					sendMessage(event.getKey(), (List) event.getValue());
					break;
				default:
					log.warn("Unknown SO event: " + event.getType());
			}
		}
		endUpdate();
	}

	@Override
	public synchronized boolean setAttribute(String name, Object value) {
		beginUpdate();
		boolean success = so.setAttribute(name, value);
		endUpdate();

		if (success) {
			Iterator<ISharedObjectListener> it = serverListeners.iterator();
			while (it.hasNext()) {
				ISharedObjectListener listener = it.next();
				listener.onSharedObjectUpdate(this, name, value);
			}
		}
		return success;
	}

	@Override
	public synchronized void setAttributes(IAttributeStore values) {
		beginUpdate();
		so.setAttributes(values);
		endUpdate();

		Iterator<ISharedObjectListener> it = serverListeners.iterator();
		while (it.hasNext()) {
			ISharedObjectListener listener = it.next();
			listener.onSharedObjectUpdate(this, values);
		}
	}

	@Override
	public synchronized void setAttributes(Map<String, Object> values) {
		beginUpdate();
		so.setAttributes(values);
		endUpdate();

		Iterator<ISharedObjectListener> it = serverListeners.iterator();
		while (it.hasNext()) {
			ISharedObjectListener listener = it.next();
			listener.onSharedObjectUpdate(this, values);
		}
	}

	@Override
	public String toString() {
		return "Shared Object: " + getName();
	}

	public synchronized void addSharedObjectListener(
			ISharedObjectListener listener) {
		serverListeners.add(listener);
	}

	public synchronized void removeSharedObjectListener(
			ISharedObjectListener listener) {
		serverListeners.remove(listener);
	}

	public void registerServiceHandler(Object handler) {
		registerServiceHandler("", handler);
	}

	public void registerServiceHandler(String name, Object handler) {
		if (name == null) {
			name = "";
		}
		handlers.put(name, handler);
	}

	public void unregisterServiceHandler() {
		unregisterServiceHandler("");
	}

	public void unregisterServiceHandler(String name) {
		if (name == null) {
			name = "";
		}
		handlers.remove(name);
	}

	public Object getServiceHandler(String name) {
		if (name == null) {
			name = "";
		}
		return handlers.get(name);
	}

	public Set<String> getServiceHandlerNames() {
		return Collections.unmodifiableSet(handlers.keySet());
	}

	/**
	 * Locks the shared object instance. Prevents any changes to this object by
	 * clients until the SharedObject.unlock() method is called.
	 */
	public void lock() {
		lock.lock();
	}

	/**
	 * Unlocks a shared object instance that was locked with
	 * SharedObject.lock().
	 */
	public void unlock() {
		lock.unlock();
	}

	/**
	 * Returns the locked state of this SharedObject.
	 * 
	 * @return true if in a locked state; false otherwise
	 */
	public boolean isLocked() {
		return lock.isLocked();
	}

	public boolean clear() {
		return so.clear();
	}

	public void close() {
		so.close();
		so = null;
	}

}