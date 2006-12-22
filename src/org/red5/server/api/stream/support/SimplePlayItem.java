package org.red5.server.api.stream.support;

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

import org.red5.server.api.stream.IPlayItem;
import org.red5.server.messaging.IMessageInput;

public class SimplePlayItem implements IPlayItem {
	private long length = -1;

	private String name;

	private long start = -2;

	private IMessageInput msgInput;

	/**
	 * Returns play item length in milliseconds
	 * 
	 * @return	Play item length in milliseconds
	 */
	public long getLength() {
		return length;
	}

	/**
	 * Returns IMessageInput object. IMessageInput is an endpoint for a consumer
	 * to connect.
	 * 
	 * @return	IMessageInput object
	 */
	public IMessageInput getMessageInput() {
		return msgInput;
	}

	/**
	 * Returns item name
	 * 
	 * @return	item name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns boolean value that specifies whether item can be played
	 */
	public long getStart() {
		return start;
	}

	/**
	 * Alias for getMessageInput
	 * 
	 * @return
	 */
	public IMessageInput getMsgInput() {
		return msgInput;
	}

	/**
     * Setter for property 'msgInput'.
     *
     * @param msgInput Value to set for property 'msgInput'.
     */
    public void setMsgInput(IMessageInput msgInput) {
		this.msgInput = msgInput;
	}

	/**
     * Setter for property 'length'.
     *
     * @param length Value to set for property 'length'.
     */
    public void setLength(long length) {
		this.length = length;
	}

	/**
     * Setter for property 'name'.
     *
     * @param name Value to set for property 'name'.
     */
    public void setName(String name) {
		this.name = name;
	}

	/**
     * Setter for property 'start'.
     *
     * @param start Value to set for property 'start'.
     */
    public void setStart(long start) {
		this.start = start;
	}

}
