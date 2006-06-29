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

import java.util.LinkedList;
import java.util.Queue;

import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.stream.IStreamData;
import org.red5.server.stream.message.RTMPMessage;

/**
 * A Play buffer for sending VOD.
 * The implementation is not synchronized.
 * @author The Red5 Project (red5@osflash.org)
 * @author Steven Gong (steven.gong@gmail.com)
 */
public class PlayBuffer {
	private long capacity;
	private long messageSize = 0;
	private Queue<RTMPMessage> messageQueue = new LinkedList<RTMPMessage>();
	
	public PlayBuffer(long capacity) {
		this.capacity = capacity;
	}
	
	/**
	 * Buffer capacity in bytes.
	 * @return
	 */
	public long getCapacity() {
		return capacity;
	}
	
	public void setCapacity(long capacity) {
		this.capacity = capacity;
	}
	
	/**
	 * Number of message in buffer.
	 * @return
	 */
	public int getMessageCount() {
		return messageQueue.size();
	}
	
	/**
	 * Total message size in bytes.
	 * @return
	 */
	public long getMessageSize() {
		return messageSize;
	}
	
	/**
	 * Put a message into this buffer.
	 * @param message
	 * @return <tt>true</tt> indicates success and <tt>false</tt>
	 * indicates buffer is full.
	 */
	public boolean putMessage(RTMPMessage message) {
		IRTMPEvent body = message.getBody();
		if (!(body instanceof IStreamData))
			throw new RuntimeException("expected IStreamData but got " + body);
		
		int size = ((IStreamData) body).getData().limit();
		if (messageSize + size > capacity) {
			return false;
		}
		messageSize += size;
		messageQueue.offer(message);
		return true;
	}
	
	/**
	 * Take a message from this buffer. The message count decreases.
	 * @return <tt>null</tt> if buffer is empty.
	 */
	public RTMPMessage takeMessage() {
		RTMPMessage message = messageQueue.poll();
		if (message != null) {
			IRTMPEvent body = message.getBody();
			if (!(body instanceof IStreamData))
				throw new RuntimeException("expected IStreamData but got " + body);
			
			messageSize -= ((IStreamData) body).getData().limit();
		}
		return message;
	}
	
	/**
	 * Peek a message but not take it from the buffer. The message count
	 * doesn't change.
	 * @return <tt>null</tt> if buffer is empty.
	 */
	public RTMPMessage peekMessage() {
		return messageQueue.peek();
	}
	
	/**
	 * Empty this buffer.
	 */
	public void clear() {
		messageQueue.clear();
		messageSize = 0;
	}
}
