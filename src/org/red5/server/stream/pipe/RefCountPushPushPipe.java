package org.red5.server.stream.pipe;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.messaging.IMessage;
import org.red5.server.messaging.IPushableConsumer;
import org.red5.server.messaging.InMemoryPushPushPipe;

public class RefCountPushPushPipe extends InMemoryPushPushPipe {
	private static final Log log = LogFactory.getLog(RefCountPushPushPipe.class);

	@Override
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
