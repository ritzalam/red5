package org.red5.server.so;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.red5.server.BasicScope;
import org.red5.server.api.IAttributeStore;
import org.red5.server.api.IScope;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.event.IEventListener;
import org.red5.server.api.so.ISharedObject;

public class SharedObjectScope extends BasicScope 
	implements ISharedObject {
	
	private final ReentrantLock lock = new ReentrantLock();
	protected SharedObject so;
	
	public SharedObjectScope(IScope parent, String name, boolean persistent){
		super(parent,TYPE, name, persistent);
		
		// Create shared object wrapper around the attributes
		// TODO: add support for true persistent SOs
		so = new SharedObject(attributes, name, persistent);
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
	}
	
	@Override
	public synchronized boolean removeAttribute(String name) {
		beginUpdate();
		boolean success = so.removeAttribute(name);
		endUpdate();
		return success;
	}

	@Override
	public synchronized void removeAttributes() {
		beginUpdate();
		so.removeAttributes();
		endUpdate();
	}

	@Override
	public void addEventListener(IEventListener listener) {
		super.addEventListener(listener);
		so.register(listener);
	}

	@Override
	public void removeEventListener(IEventListener listener) {
		so.unregister(listener);
		super.removeEventListener(listener);
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
		return success;
	}

	@Override
	public synchronized void setAttributes(IAttributeStore values) {
		beginUpdate();
		so.setAttributes(values);
		endUpdate();
	}

	@Override
	public synchronized void setAttributes(Map<String, Object> values) {
		beginUpdate();
		so.setAttributes(values);
		endUpdate();
	}
	
}