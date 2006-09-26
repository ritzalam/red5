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

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.Map;

/**
 * Event object corresponds to the connect/disconnect events
 * among providers/consumers and pipes.
 *  
 * @author The Red5 Project (red5@osflash.org)
 * @author Steven Gong (steven.gong@gmail.com)
 */
public class PipeConnectionEvent extends EventObject {
	private static final long serialVersionUID = 9078843765378168072L;

	private List<Runnable> taskList = new ArrayList<Runnable>();

	/**
	 * A provider connects as pull mode.
	 */
	public static final int PROVIDER_CONNECT_PULL = 0;

	/**
	 * A provider connects as push mode.
	 */
	public static final int PROVIDER_CONNECT_PUSH = 1;

	/**
	 * A provider disconnects.
	 */
	public static final int PROVIDER_DISCONNECT = 2;

	/**
	 * A consumer connects as pull mode.
	 */
	public static final int CONSUMER_CONNECT_PULL = 3;

	/**
	 * A consumer connects as push mode.
	 */
	public static final int CONSUMER_CONNECT_PUSH = 4;

	/**
	 * A consumer disconnects.
	 */
	public static final int CONSUMER_DISCONNECT = 5;

	private IProvider provider;

	private IConsumer consumer;

	private int type;

	private Map paramMap;

	/**
	 * Construct an object with the specific pipe as the
	 * <tt>source</tt>
	 * @param source A pipe that triggers this event.
	 */
	public PipeConnectionEvent(Object source) {
		super(source);
	}

	public IProvider getProvider() {
		return provider;
	}

	public void setProvider(IProvider provider) {
		this.provider = provider;
	}

	public IConsumer getConsumer() {
		return consumer;
	}

	public void setConsumer(IConsumer consumer) {
		this.consumer = consumer;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public Map getParamMap() {
		return paramMap;
	}

	public void setParamMap(Map paramMap) {
		this.paramMap = paramMap;
	}

	public void addTask(Runnable task) {
		taskList.add(task);
	}

	List<Runnable> getTaskList() {
		return taskList;
	}
}
