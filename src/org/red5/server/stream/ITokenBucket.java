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

/**
 * A token bucket that is used to control bandwidth.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Steven Gong (steven.gong@gmail.com)
 */
public interface ITokenBucket {
	/**
	 * Acquire tokens amount of <tt>tokenCount</tt>
	 * waiting <tt>wait</tt> milliseconds if token not available.
	 * @param tokenCount The count of tokens to acquire.
	 * @param wait Milliseconds to wait. <tt>0</tt> means no wait
	 * and any value below zero means wait forever. 
	 * @return <tt>true</tt> if successfully acquired or <tt>false</tt>
	 * if not acquired.
	 */
	boolean acquireToken(double tokenCount, long wait);

	/**
	 * Nonblockingly acquire token. If the token is not available and
	 * <tt>task</tt> is not null, the callback will be executed when the token
	 * is available. The tokens are not consumed automatically before callback,
	 * so it's recommended to acquire token again in callback function.
	 * 
	 * @param tokenCount
	 * @param callback
	 * @return <tt>true</tt> if successfully acquired or <tt>false</tt>
	 * if not acquired.
	 */
	boolean acquireTokenNonblocking(double tokenCount,
			ITokenBucketCallback callback);

	/**
	 * Nonblockingly acquire token. The upper limit is specified. If
	 * not enough tokens are left in bucket, all remaining will be
	 * returned.
	 * @param upperLimitCount
	 * @return
	 */
	double acquireTokenBestEffort(double upperLimitCount);

	/**
	 * Get the capacity of this bucket in Byte.
	 * 
	 * @return
	 */
	long getCapacity();

	/**
	 * The amount of tokens increased per second in millisecond.
	 * 
	 * @return
	 */
	double getSpeed();

	/**
	 * Reset this token bucket. All pending threads are woken up with <tt>false</tt>
	 * returned for acquiring token and callback is removed w/o calling back.
	 */
	void reset();

	public interface ITokenBucketCallback {
		void available(ITokenBucket bucket, double tokenCount);

		void reset(ITokenBucket bucket, double tokenCount);
	}
}
