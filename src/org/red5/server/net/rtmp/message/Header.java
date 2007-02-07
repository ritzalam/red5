package org.red5.server.net.rtmp.message;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2007 by respective authors (see below). All rights reserved.
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

/**
 * RTMP packet header
 */
public class Header implements Constants {
    /**
     * Channel
     */
	private byte channelId;
    /**
     * Timer
     */
	private int timer;
    /**
     * Header size
     */
	private int size;
    /**
     * Type of data
     */
	private byte dataType;
    /**
     * Stream id
     */
	private int streamId;
    /**
     * Whether timer value is relative
     */
	private boolean timerRelative = true;

	/**
     * Getter for channel id
     *
     * @return  Channel id
     */
    public byte getChannelId() {
		return channelId;
	}

	/**
     * Setter for channel id
     *
     * @param channelId  Header channel id
     */
    public void setChannelId(byte channelId) {
		this.channelId = channelId;
	}

	/**
     * Getter for data type
     *
     * @return  Data type
     */
    public byte getDataType() {
		return dataType;
	}

	/**
     * Setter for data type
     *
     * @param dataType  Data type
     */
    public void setDataType(byte dataType) {
		this.dataType = dataType;
	}

	/**
     * Getter for size.
     *
     * @return  Header size
     */
    public int getSize() {
		return size;
	}

	/**
     * Setter for size
     *
     * @param size  Header size
     */
    public void setSize(int size) {
		this.size = size;
	}

	/**
     * Getter for stream id
     *
     * @return  Stream id
     */
    public int getStreamId() {
		return streamId;
	}

	/**
     * Setter for stream id
     *
     * @param streamId  Stream id
     */
    public void setStreamId(int streamId) {
		this.streamId = streamId;
	}

	/**
     * Getter for timer
     *
     * @return  Timer
     */
    public int getTimer() {
		return timer;
	}

	/**
     * Setter for timer
     *
     * @param timer  Timer
     */
    public void setTimer(int timer) {
		this.timer = timer;
	}

	/**
     * Getter for timer relative flag
     *
     * @return  <code>true</code> if timer value is relative, <code>false</code> otherwise
     */
    public boolean isTimerRelative() {
		return timerRelative;
	}

	/**
     * Setter for timer relative flag
     *
     * @param timerRelative <code>true</code> if timer values are relative, <code>false</code> otherwise
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
