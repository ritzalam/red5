package org.red5.server.so;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.red5.server.BasicScope;
import org.red5.server.api.IAttributeStore;
import org.red5.server.api.IScope;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.event.IEventListener;
import org.red5.server.api.persistence.IPersistenceStore;
import org.red5.server.api.so.ISharedObject;
import org.red5.server.api.so.ISharedObjectListener;

public class SharedObjectScope extends BasicScope 
	implements ISharedObject {
	
	private final ReentrantLock lock = new ReentrantLock();
	private HashSet<ISharedObjectListener> serverListeners = new HashSet<ISharedObjectListener>();
	protected SharedObject so;
	
	public SharedObjectScope(IScope parent, String name, boolean persistent, IPersistenceStore store){
		super(parent,TYPE, name, persistent);
		
		// Create shared object wrapper around the attributes
		so = (SharedObject) store.load(SharedObject.PERSISTENCE_TYPE + "/" + parent.getContextPath() + "/" + name);
		if (so == null) {
			so = new SharedObject(attributes, name, parent.getContextPath(), persistent, store);

			store.save(so);
		} else {
			so.setName(name);
			so.setPath(parent.getContextPath());
		}
	}
	
	public void setPersistenceClass(String persistenceClass) {
		// Nothing to do here, the shared object will take care of persistence.
	}
	
	public boolean isPersistentObject() {
		return so.isPersistentObject();
	}
	
	public void beginUpdate() {
		if (!lock.isHeldByCurrentThread())
			lock.lock();
		
		so.beginUpdate();
	}

	public void beginUpdate(IEventListener listener) {
		if (!lock.isHeldByCurrentThread())
			lock.lock();
		
		so.beginUpdate(listener);
	}

	public void endUpdate() {
		so.endUpdate();
		if (so.updateCounter == 0)
			lock.unlock();
	}

	public int getVersion() {
		return so.getVersion();
	}

	public void sendMessage(String handler, List arguments) {
		beginUpdate();
		so.sendMessage(handler, arguments);
		endUpdate();
		
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
		
		Iterator<ISharedObjectListener> it = serverListeners.iterator();
		while (it.hasNext()) {
			ISharedObjectListener soListener = it.next();
			soListener.onSharedObjectDisconnect(this);
		}
	}

	public Map<String, Object> getData() {
		return so.getData();
	}

	public boolean handleEvent(IEvent e){
		if(! (e instanceof ISharedObjectEvent)) return false;
		ISharedObjectMessage msg = (ISharedObjectMessage) e;
		if (msg.hasSource()) beginUpdate(msg.getSource());
		else beginUpdate();
		for(ISharedObjectEvent event : msg.getEvents()){
			switch(event.getType()){
			case CONNECT:
				if(msg.hasSource()) 
					addEventListener(msg.getSource());
				break;
			case SET_ATTRIBUTE:
				setAttribute(event.getKey(), event.getValue());
				break;
			case DELETE_ATTRIBUTE:
				removeAttribute(event.getKey());
				break;
			case SEND_MESSAGE:
				sendMessage(event.getKey(), (List) event.getValue());
				break;
			case CLEAR:
				removeAttributes();
				break;		
			}
		}
		endUpdate();
		return true;
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
		return "Shared Object: "+getName();
	}
	
	public synchronized void addSharedObjectListener(ISharedObjectListener listener) {
		serverListeners.add(listener);
	}
	
	public synchronized void removeSharedObjectListener(ISharedObjectListener listener) {
		serverListeners.remove(listener);
	}
	
}