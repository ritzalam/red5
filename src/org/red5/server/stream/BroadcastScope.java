package org.red5.server.stream;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.BasicScope;
import org.red5.server.api.IScope;
import org.red5.server.messaging.IConsumer;
import org.red5.server.messaging.IMessage;
import org.red5.server.messaging.IPipeConnectionListener;
import org.red5.server.messaging.IProvider;
import org.red5.server.messaging.InMemoryPushPushPipe;
import org.red5.server.messaging.OOBControlMessage;
import org.red5.server.stream.pipe.RefCountPushPushPipe;

public class BroadcastScope extends BasicScope implements IBroadcastScope {
	private static final Log log = LogFactory.getLog(BroadcastScope.class);
	
	private RefCountPushPushPipe pipe;
	private int compCounter;
	private boolean hasRemoved;
	
	public BroadcastScope(IScope parent, String name) {
		super(parent, TYPE, name, false);
		pipe = new RefCountPushPushPipe();
		compCounter = 0;
		hasRemoved = false;
	}

	public void addPipeConnectionListener(IPipeConnectionListener listener) {
		pipe.addPipeConnectionListener(listener);
	}

	public void removePipeConnectionListener(IPipeConnectionListener listener) {
		pipe.removePipeConnectionListener(listener);
	}

	public IMessage pullMessage() {
		return pipe.pullMessage();
	}

	public IMessage pullMessage(long wait) {
		return pipe.pullMessage(wait);
	}

	synchronized public boolean subscribe(IConsumer consumer, Map paramMap) {
		if (hasRemoved) return false;
		boolean result = pipe.subscribe(consumer, paramMap);
		if (result) compCounter++;
		return result;
	}

	synchronized public boolean unsubscribe(IConsumer consumer) {
		boolean result = pipe.unsubscribe(consumer);
		if (result) compCounter--;
		if (compCounter <= 0) removeSelf();
		return result;
	}

	public void sendOOBControlMessage(IConsumer consumer,
			OOBControlMessage oobCtrlMsg) {
		pipe.sendOOBControlMessage(consumer, oobCtrlMsg);
	}

	public void pushMessage(IMessage message) {
		pipe.pushMessage(message);
	}

	synchronized public boolean subscribe(IProvider provider, Map paramMap) {
		if (hasRemoved) return false;
		boolean result = pipe.subscribe(provider, paramMap);
		if (result) compCounter++;
		return result;
	}

	synchronized public boolean unsubscribe(IProvider provider) {
		boolean result = pipe.unsubscribe(provider);
		if (result) compCounter--;
		if (compCounter <= 0) removeSelf();
		return result;
	}

	public void sendOOBControlMessage(IProvider provider,
			OOBControlMessage oobCtrlMsg) {
		pipe.sendOOBControlMessage(provider, oobCtrlMsg);
	}

	/**
	 * Remove self from parent scope.
	 */
	private void removeSelf() {
		if (compCounter <= 0) {
			getParent().removeChildScope(this);
			hasRemoved = true;
		}
	}
}
