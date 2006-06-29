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
import org.red5.server.messaging.OOBControlMessage;
import org.red5.server.messaging.PipeConnectionEvent;
import org.red5.server.stream.pipe.RefCountPushPushPipe;

public class BroadcastScope extends BasicScope implements IBroadcastScope, IPipeConnectionListener {
	private static final Log log = LogFactory.getLog(BroadcastScope.class);
	
	private RefCountPushPushPipe pipe;
	private int compCounter;
	private boolean hasRemoved;
	
	public BroadcastScope(IScope parent, String name) {
		super(parent, TYPE, name, false);
		pipe = new RefCountPushPushPipe();
		pipe.addPipeConnectionListener(this);
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

	public boolean subscribe(IConsumer consumer, Map paramMap) {
		synchronized (pipe) {
			if (hasRemoved) return false;
			return pipe.subscribe(consumer, paramMap);
		}
	}
	
	public boolean unsubscribe(IConsumer consumer) {
		return pipe.unsubscribe(consumer);
	}

	public void sendOOBControlMessage(IConsumer consumer,
			OOBControlMessage oobCtrlMsg) {
		pipe.sendOOBControlMessage(consumer, oobCtrlMsg);
	}

	public void pushMessage(IMessage message) {
		pipe.pushMessage(message);
	}

	synchronized public boolean subscribe(IProvider provider, Map paramMap) {
		synchronized (pipe) {
			if (hasRemoved) return false;
			return pipe.subscribe(provider, paramMap);
		}
	}

	synchronized public boolean unsubscribe(IProvider provider) {
		return pipe.unsubscribe(provider);
	}

	public void sendOOBControlMessage(IProvider provider,
			OOBControlMessage oobCtrlMsg) {
		pipe.sendOOBControlMessage(provider, oobCtrlMsg);
	}

	public void onPipeConnectionEvent(PipeConnectionEvent event) {
		switch(event.getType()) {
			case PipeConnectionEvent.CONSUMER_CONNECT_PULL:
			case PipeConnectionEvent.CONSUMER_CONNECT_PUSH:
			case PipeConnectionEvent.PROVIDER_CONNECT_PULL:
			case PipeConnectionEvent.PROVIDER_CONNECT_PUSH:
				compCounter++;
				break;

			case PipeConnectionEvent.CONSUMER_DISCONNECT:
			case PipeConnectionEvent.PROVIDER_DISCONNECT:
				compCounter--;
				if (compCounter <= 0) {
					if (hasParent())
						getParent().removeChildScope(this);
					hasRemoved = true;
				}
				break;
		}
	}
	
}
