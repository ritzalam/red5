package org.red5.server.messaging;

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

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A simple in-memory version of push-push pipe.
 * It is triggered by an active provider to push messages
 * through it to an event-driven consumer.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Steven Gong (steven.gong@gmail.com)
 */
public class InMemoryPushPushPipe extends AbstractPipe {
	private static final Log log = LogFactory.getLog(InMemoryPushPushPipe.class);
	
	public boolean subscribe(IConsumer consumer, Map paramMap) {
		if (!(consumer instanceof IPushableConsumer)) {
			throw new IllegalArgumentException("Non-pushable consumer not supported by PushPushPipe");
		}
		boolean success = super.subscribe(consumer, paramMap);
		if (success) fireConsumerConnectionEvent(consumer, PipeConnectionEvent.CONSUMER_CONNECT_PUSH, paramMap);
		return success;
	}

	public boolean subscribe(IProvider provider, Map paramMap) {
		boolean success = super.subscribe(provider, paramMap);
		if (success) fireProviderConnectionEvent(provider, PipeConnectionEvent.PROVIDER_CONNECT_PUSH, paramMap);
		return success;
	}

	public IMessage pullMessage() {
		return null;
	}

	public IMessage pullMessage(long wait) {
		return null;
	}

	public void pushMessage(IMessage message) {
		synchronized (consumers) {
			for (Iterator iter = consumers.iterator(); iter.hasNext(); ) {
				IPushableConsumer consumer = (IPushableConsumer) iter.next();
				try {
					consumer.pushMessage(this, message);
				} catch (Throwable t) {
					log.error("exception when pushing message to consumer", t);
				}
			}
		}
	}
}
