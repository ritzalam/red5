package org.red5.server.net.rtmp.event;

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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Simple allocation debugger for Event reference counting.
 * @author The Red5 Project (red5@osflash.org)
 * @author Steven Gong (steven.gong@gmail.com) on behalf of (ce@publishing-etc.de)
 */
public class AllocationDebugger {
	
	private class Info {

		public int refcount;
		
		public Info() {
			refcount = 1;
		}
		
	}
	
	private static AllocationDebugger instance;
	
	public static AllocationDebugger getInstance() {
		if (instance == null) instance = new AllocationDebugger();
		return instance;
	}
	
	private Log log;
	
	private Map<BaseEvent,Info> events;	
	
	private AllocationDebugger() {
		log = LogFactory.getLog(getClass().getName());
		events = new HashMap<BaseEvent,Info>();
	}
	
	protected synchronized void create(BaseEvent event) {
		events.put(event,new Info());
	}
	
	protected synchronized void retain(BaseEvent event) {
		Info info = events.get(event);
		if(info != null) {
			info.refcount++;
		} else {
			log.warn("Retain called on already released event.");
		}
	}
	
	protected synchronized void release(BaseEvent event) {
		Info info = events.get(event);
		if (info != null) {
			info.refcount--;
			if (info.refcount == 0) {
				events.remove(event);
			}
		} else {
			log.warn("Release called on already released event.");
		}
	}
	
	public synchronized void dump() {
		log.debug("dumping allocations "+events.size());
		for (Entry<BaseEvent, Info> entry : events.entrySet()) {
			log.debug(entry.getKey() + " " + entry.getValue().refcount);
		}
	}
	
}
