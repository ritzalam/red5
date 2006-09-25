package org.red5.server.stream;

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

import java.util.List;
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
import org.red5.server.messaging.PipeConnectionEvent;

public class BroadcastScope extends BasicScope implements IBroadcastScope,
		IPipeConnectionListener {
	private static final Log log = LogFactory.getLog(BroadcastScope.class);
	
	private InMemoryPushPushPipe pipe;
	private int compCounter;
	private boolean hasRemoved;
	
	public BroadcastScope(IScope parent, String name) {
		super(parent, TYPE, name, false);
		pipe = new InMemoryPushPushPipe();
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
			if (hasRemoved)
				return false;
			return pipe.subscribe(consumer, paramMap);
		}
	}
	
	public boolean unsubscribe(IConsumer consumer) {
		return pipe.unsubscribe(consumer);
	}
	
	public List<IConsumer> getConsumers() {
		return pipe.getConsumers();
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
			if (hasRemoved)
				return false;
			return pipe.subscribe(provider, paramMap);
		}
	}

	synchronized public boolean unsubscribe(IProvider provider) {
		return pipe.unsubscribe(provider);
	}
	
	public List<IProvider> getProviders() {
		return pipe.getProviders();
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
					// XXX should we synchronize parent before removing?
					if (hasParent()) {
					IProviderService providerService = (IProviderService) getParent()
							.getContext().getBean(IProviderService.KEY);
					providerService.unregisterBroadcastStream(getParent(),
							getName());
					}
					hasRemoved = true;
				}
				break;
		}
	}
	
}
