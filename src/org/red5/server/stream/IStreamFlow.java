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

import org.red5.server.stream.message.RTMPMessage;

/**
 * Stream flow object contains information about buffering, bit rate of stream, segmentation, etc
 */
public interface IStreamFlow {

	/**
     * Getter for property 'maxTimeBuffer'.
     *
     * @return Value for property 'maxTimeBuffer'.
     */
    public int getMaxTimeBuffer();

	/**
     * Setter for property 'maxTimeBuffer'.
     *
     * @param maxTimeBuffer Value to set for property 'maxTimeBuffer'.
     */
    public void setMaxTimeBuffer(int maxTimeBuffer);

	/**
     * Getter for property 'minTimeBuffer'.
     *
     * @return Value for property 'minTimeBuffer'.
     */
    public int getMinTimeBuffer();

	/**
     * Setter for property 'minTimeBuffer'.
     *
     * @param minTimeBuffer Value to set for property 'minTimeBuffer'.
     */
    public void setMinTimeBuffer(int minTimeBuffer);

	/**
     * Getter for property 'clientTimeBuffer'.
     *
     * @return Value for property 'clientTimeBuffer'.
     */
    public long getClientTimeBuffer();

	/**
     * Setter for property 'clientTimeBuffer'.
     *
     * @param clientTimeBuffer Value to set for property 'clientTimeBuffer'.
     */
    public void setClientTimeBuffer(long clientTimeBuffer);

	/**
     * Getter for property 'dataBitRate'.
     *
     * @return Value for property 'dataBitRate'.
     */
    public int getDataBitRate();

	/**
     * Getter for property 'segmentBytesTransfered'.
     *
     * @return Value for property 'segmentBytesTransfered'.
     */
    public int getSegmentBytesTransfered();

	/**
     * Getter for property 'segmentDataTime'.
     *
     * @return Value for property 'segmentDataTime'.
     */
    public int getSegmentDataTime();

	/**
     * Getter for property 'segmentStreamTime'.
     *
     * @return Value for property 'segmentStreamTime'.
     */
    public long getSegmentStreamTime();

	/**
     * Getter for property 'streamBitRate'.
     *
     * @return Value for property 'streamBitRate'.
     */
    public int getStreamBitRate();

	/**
     * Getter for property 'bufferTimeIncreasing'.
     *
     * @return Value for property 'bufferTimeIncreasing'.
     */
    public boolean isBufferTimeIncreasing();

	/**
     * Getter for property 'totalBytesTransfered'.
     *
     * @return Value for property 'totalBytesTransfered'.
     */
    public long getTotalBytesTransfered();

	/**
     * Getter for property 'totalDataTime'.
     *
     * @return Value for property 'totalDataTime'.
     */
    public long getTotalDataTime();

	/**
     * Getter for property 'totalStreamTime'.
     *
     * @return Value for property 'totalStreamTime'.
     */
    public long getTotalStreamTime();

	/**
     * Getter for property 'bufferTime'.
     *
     * @return Value for property 'bufferTime'.
     */
    public int getBufferTime();

	public void reset();

	public void pause();

	public void resume();

	public void clear();

	public void update(RTMPMessage msg);

	/**
     * Getter for property 'zeroToStreamTime'.
     *
     * @return Value for property 'zeroToStreamTime'.
     */
    public long getZeroToStreamTime();

}