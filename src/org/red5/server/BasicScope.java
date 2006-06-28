package org.red5.server;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.red5.server.api.IBasicScope;
import org.red5.server.api.IScope;
import org.red5.server.api.ScopeUtils;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.event.IEventListener;

public class BasicScope extends PersistableAttributeStore implements IBasicScope {

	protected IScope parent;
	protected Set<IEventListener> listeners;
	protected String persistenceClass = null; 
	
	public BasicScope(IScope parent, String type, String name, boolean persistent){
		super(type, name, null, persistent);
		this.parent = parent;
		this.listeners = new HashSet<IEventListener>();
	}
	
	public boolean hasParent() {
		return true;
	}

	public IScope getParent() {
		return parent;
	}

	public int getDepth() {
		return parent.getDepth() + 1;
	}
	
	@Override
	public String getPath() {
		return parent.getPath() + "/" + parent.getName();
	}

	public void addEventListener(IEventListener listener) {
		listeners.add(listener);
	}

	public void removeEventListener(IEventListener listener) {
		listeners.remove(listener);
		if(ScopeUtils.isRoom(this) && isPersistent() && listeners.isEmpty()){
			// Delete empty rooms
			parent.removeChildScope(this);
		} 
	}

	public Iterator<IEventListener> getEventListeners() {
		return listeners.iterator();
	}

	public void setPersistent(boolean persistent) {
		this.persistent = persistent;
	}

	public boolean handleEvent(IEvent event) {
		// do nothing.
		return false;
	}

	public void notifyEvent(IEvent event) {
		// TODO Auto-generated method stub
		
	}

	public void dispatchEvent(IEvent event){
		for(IEventListener listener : listeners){
			if(event.getSource()==null || 
					event.getSource() != listener )
				listener.notifyEvent(event);
		}
	}

	public Iterator<IBasicScope> iterator() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public class EmptyBasicScopeIterator implements Iterator<IBasicScope>{

		public boolean hasNext() {
			return false;
		}

		public IBasicScope next() {
			return null;
		}

		public void remove() {
			// nothing
		}
		
	}
	
	
}