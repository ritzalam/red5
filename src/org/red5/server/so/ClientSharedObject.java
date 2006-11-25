package org.red5.server.so;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.BaseConnection;
import org.red5.server.api.IConnection;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.event.IEventDispatcher;
import org.red5.server.api.event.IEventListener;
import org.red5.server.api.so.IClientSharedObject;
import org.red5.server.api.so.ISharedObjectListener;
import org.red5.server.net.rtmp.Channel;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.so.ISharedObjectEvent.Type;

public class ClientSharedObject extends SharedObject implements
		IClientSharedObject, IEventDispatcher {

	protected static Log log = LogFactory.getLog(ClientSharedObject.class.getName());

	private boolean initialSyncReceived = false;
	private final ReentrantLock lock = new ReentrantLock();
	private HashSet<ISharedObjectListener> listeners = new HashSet<ISharedObjectListener>();
	private HashMap<String, Object> handlers = new HashMap<String, Object>();

	public ClientSharedObject(String name, boolean persistent) {
		super();
		this.name = name;
		persistentSO = persistent;
	}
	
	/**
	 * Connect the shared object using the passed connection.
	 * 
	 * @param conn
	 */
	public void connect(IConnection conn) {
		if (!(conn instanceof RTMPConnection))
			throw new RuntimeException("can only connect through RTMP connections");
		
		if (isConnected())
			throw new RuntimeException("already connected");
		
		source = conn;
		SharedObjectMessage msg = new SharedObjectMessage(name, 0, isPersistentObject());
		msg.addEvent(new SharedObjectEvent(Type.SERVER_CONNECT, null, null));
		Channel c = ((RTMPConnection) conn).getChannel((byte) 3);
		c.write(msg);
	}
	
	/**
	 * Disconnect the shared object.
	 */
	public void disconnect() {
		SharedObjectMessage msg = new SharedObjectMessage(name, 0, isPersistentObject());
		msg.addEvent(new SharedObjectEvent(Type.SERVER_DISCONNECT, null, null));
		Channel c = ((RTMPConnection) source).getChannel((byte) 3);
		c.write(msg);
		notifyDisconnect();
	}
	
	public boolean isConnected() {
		return initialSyncReceived;
	}
	
	public void addSharedObjectListener(ISharedObjectListener listener) {
		listeners.add(listener);
	}

	public void removeSharedObjectListener(ISharedObjectListener listener) {
		listeners.remove(listener);
	}

	public void dispatchEvent(IEvent e) {
		if (e.getType() != IEvent.Type.SHARED_OBJECT
				|| !(e instanceof ISharedObjectMessage)) {
			// Don't know how to handle this event.
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
				case CLIENT_INITIAL_DATA:
					initialSyncReceived = true;
					notifyConnect();
					break;
					
				case CLIENT_CLEAR_DATA:
					data.clear();
					notifyClear();
					break;
				
				case CLIENT_DELETE_DATA:
				case CLIENT_DELETE_ATTRIBUTE:
					data.remove(event.getKey());
					notifyDelete(event.getKey());
					break;
					
				case CLIENT_SEND_MESSAGE:
					notifySendMessage(event.getKey(), (List) event.getValue());
					break;
					
				case CLIENT_UPDATE_DATA:
					data.putAll((Map<String, Object>) event.getValue());
					notifyUpdate(event.getKey(), (Map<String, Object>) event.getValue());
					break;
					
				case CLIENT_UPDATE_ATTRIBUTE:
					data.put(event.getKey(), event.getValue());
					notifyUpdate(event.getKey(), event.getValue());
					break;
					
				default:
					log.warn("Unknown SO event: " + event.getType());
			}
		}
		endUpdate();
	}

	protected void notifyConnect() {
		Iterator<ISharedObjectListener> it = listeners.iterator();
		while (it.hasNext()) {
			ISharedObjectListener listener = it.next();
			listener.onSharedObjectConnect(this);
		}
	}
	
	protected void notifyDisconnect() {
		Iterator<ISharedObjectListener> it = listeners.iterator();
		while (it.hasNext()) {
			ISharedObjectListener listener = it.next();
			listener.onSharedObjectDisconnect(this);
		}
	}
	
	protected void notifyUpdate(String key, Object value) {
		Iterator<ISharedObjectListener> it = listeners.iterator();
		while (it.hasNext()) {
			ISharedObjectListener listener = it.next();
			listener.onSharedObjectUpdate(this, key, value);
		}
	}
	
	protected void notifyUpdate(String key, Map<String, Object> value) {
		if (value.size() == 1) {
			Map.Entry<String, Object> entry = value.entrySet().iterator().next();
			notifyUpdate(entry.getKey(), entry.getValue());
			return;
		}
		Iterator<ISharedObjectListener> it = listeners.iterator();
		while (it.hasNext()) {
			ISharedObjectListener listener = it.next();
			listener.onSharedObjectUpdate(this, key, value);
		}
	}
	
	protected void notifyDelete(String key) {
		Iterator<ISharedObjectListener> it = listeners.iterator();
		while (it.hasNext()) {
			ISharedObjectListener listener = it.next();
			listener.onSharedObjectDelete(this, key);
		}
	}

	protected void notifyClear() {
		Iterator<ISharedObjectListener> it = listeners.iterator();
		while (it.hasNext()) {
			ISharedObjectListener listener = it.next();
			listener.onSharedObjectClear(this);
		}
	}

	protected void notifySendMessage(String method, List params) {
		Iterator<ISharedObjectListener> it = listeners.iterator();
		while (it.hasNext()) {
			ISharedObjectListener listener = it.next();
			listener.onSharedObjectSend(this, method, params);
		}
	}

	@Override
	public synchronized boolean setAttribute(String name, Object value) {
		ownerMessage.addEvent(Type.SERVER_SET_ATTRIBUTE, name, null);
		notifyModified();
		return true;
	}

	@Override
	public synchronized boolean removeAttribute(String name) {
		ownerMessage.addEvent(Type.SERVER_DELETE_ATTRIBUTE, name, null);
		notifyModified();
		return true;
	}

	@Override
	public synchronized void sendMessage(String handler, List arguments) {
		ownerMessage.addEvent(Type.SERVER_SEND_MESSAGE, handler, arguments);
		notifyModified();
	}

	@Override
	public synchronized void removeAttributes() {
		// TODO: there must be a direct way to clear the SO on the client
		// side...
		Iterator keys = data.keySet().iterator();
		while (keys.hasNext()) {
			String key = (String) keys.next();
			ownerMessage.addEvent(Type.SERVER_DELETE_ATTRIBUTE, key, null);
		}
		notifyModified();
	}

	@Override
	public synchronized void beginUpdate() {
		if (!lock.isHeldByCurrentThread()) {
			lock.lock();
		}
		super.beginUpdate();
	}

	@Override
	public synchronized void beginUpdate(IEventListener listener) {
		if (!lock.isHeldByCurrentThread()) {
			lock.lock();
		}
		super.beginUpdate(listener);
	}

	@Override
	public synchronized void endUpdate() {
		super.endUpdate();
		if (updateCounter == 0) {
			lock.unlock();
		}
	}

	public void lock() {
		lock.lock();
	}

	public void unlock() {
		lock.unlock();
	}

	public boolean isLocked() {
		return lock.isLocked();
	}

	public void registerServiceHandler(Object handler) {
		registerServiceHandler("", handler);
	}

	public void unregisterServiceHandler(String name) {
		handlers.remove(name);
	}

	public void registerServiceHandler(String name, Object handler) {
		if (name == null) {
			name = "";
		}
		handlers.put(name, handler);
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

	public Boolean getBoolAttribute(String name) {
		return (Boolean) getAttribute(name);
	}

	public Byte getByteAttribute(String name) {
		return (Byte) getAttribute(name);
	}

	public Double getDoubleAttribute(String name) {
		return (Double) getAttribute(name);
	}

	public Integer getIntAttribute(String name) {
		return (Integer) getAttribute(name);
	}

	public List getListAttribute(String name) {
		return (List) getAttribute(name);
	}

	public Long getLongAttribute(String name) {
		return (Long) getAttribute(name);
	}

	public Map getMapAttribute(String name) {
		return (Map) getAttribute(name);
	}

	public Set getSetAttribute(String name) {
		return (Set) getAttribute(name);
	}

	public Short getShortAttribute(String name) {
		return (Short) getAttribute(name);
	}

	public String getStringAttribute(String name) {
		return (String) getAttribute(name);
	}

	synchronized public Object getAttribute(String name, Object defaultValue) {
		if (!hasAttribute(name)) {
			setAttribute(name, defaultValue);
		}

		return getAttribute(name);
	}

}
