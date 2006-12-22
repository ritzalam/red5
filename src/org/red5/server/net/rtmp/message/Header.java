package org.red5.server.net.rtmp.message;

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

public class Header implements Constants {

	private byte channelId = 0;

	private int timer = 0;

	private int size = 0;

	private byte dataType = 0;

	private int streamId = 0;

	private boolean timerRelative = true;

	/**
     * Getter for property 'channelId'.
     *
     * @return Value for property 'channelId'.
     */
    public byte getChannelId() {
		return channelId;
	}

	/**
     * Setter for property 'channelId'.
     *
     * @param channelId Value to set for property 'channelId'.
     */
    public void setChannelId(byte channelId) {
		this.channelId = channelId;
	}

	/**
     * Getter for property 'dataType'.
     *
     * @return Value for property 'dataType'.
     */
    public byte getDataType() {
		return dataType;
	}

	/**
     * Setter for property 'dataType'.
     *
     * @param dataType Value to set for property 'dataType'.
     */
    public void setDataType(byte dataType) {
		this.dataType = dataType;
	}

	/**
     * Getter for property 'size'.
     *
     * @return Value for property 'size'.
     */
    public int getSize() {
		return size;
	}

	/**
     * Setter for property 'size'.
     *
     * @param size Value to set for property 'size'.
     */
    public void setSize(int size) {
		this.size = size;
	}

	/**
     * Getter for property 'streamId'.
     *
     * @return Value for property 'streamId'.
     */
    public int getStreamId() {
		return streamId;
	}

	/**
     * Setter for property 'streamId'.
     *
     * @param streamId Value to set for property 'streamId'.
     */
    public void setStreamId(int streamId) {
		this.streamId = streamId;
	}

	/**
     * Getter for property 'timer'.
     *
     * @return Value for property 'timer'.
     */
    public int getTimer() {
		return timer;
	}

	/**
     * Setter for property 'timer'.
     *
     * @param timer Value to set for property 'timer'.
     */
    public void setTimer(int timer) {
		this.timer = timer;
	}

	/**
     * Getter for property 'timerRelative'.
     *
     * @return Value for property 'timerRelative'.
     */
    public boolean isTimerRelative() {
		return timerRelative;
	}

	/**
     * Setter for property 'timerRelative'.
     *
     * @param timerRelative Value to set for property 'timerRelative'.
     */
    public void setTimerRelative(boolean timerRelative) {
		this.timerRelative = timerRelative;
	}

	/** {@inheritDoc} */
    @Override
	public boolean equals(Object other) {
		if (!(other instanceof Header)) {
			return false;
		}
		final Header header = (Header) other;
		return (header.getChannelId() == channelId
				&& header.getDataType() == dataType && header.getSize() == size
				&& header.getTimer() == timer && header.getStreamId() == streamId);
	}

	/** {@inheritDoc} */
    @Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("ChannelId: ").append(channelId).append(", ");
		sb.append("Timer: ").append(timer).append(" (" + (timerRelative ? "relative" : "absolute") + ')').append(", ");
		sb.append("Size: ").append(size).append(", ");
		sb.append("DateType: ").append(dataType).append(", ");
		sb.append("StreamId: ").append(streamId);
		return sb.toString();
	}

	/** {@inheritDoc} */
    @Override
	public Object clone() {
		final Header header = new Header();
		header.setChannelId(channelId);
		header.setTimer(timer);
		header.setSize(size);
		header.setDataType(dataType);
		header.setStreamId(streamId);
		return header;
	}

}
