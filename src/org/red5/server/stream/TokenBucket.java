package org.red5.server.stream;

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

public class TokenBucket implements ITokenBucket {
    /**
     *
     */
    private double speed;
    /**
     *
     */
	private long capacity;
    /**
     * 
     */
	private double tokens;
    /**
     *
     */
	private WaitObject waitObject;

	/** Constructs a new TokenBucket. */
    public TokenBucket() {
	}

    /**
     * Creates new Token bucket with initial tockens
     * @param initialTokens    Initial tokens
     */
	public TokenBucket(double initialTokens) {
		tokens = initialTokens;
	}

	/** {@inheritDoc} */
    public synchronized boolean acquireToken(double tokenCount, long wait) {
		if (wait > 0) {
			// as of now, we don't support blocking mode
			throw new IllegalArgumentException("blocking wait unsupported");
		}
		if (tokens >= tokenCount) {
			tokens -= tokenCount;
			return true;
		} else {
			return false;
		}
	}

	/** {@inheritDoc} */
    public synchronized boolean acquireTokenNonblocking(double tokenCount,
			ITokenBucketCallback callback) {
		// TODO use a wait queue instead
		if (waitObject != null) {
			return false;
		}
		if (tokens >= tokenCount) {
			tokens -= tokenCount;
			return true;
		} else {
			if (callback != null) {
				waitObject = new WaitObject();
				waitObject.callback = callback;
				waitObject.tokenCount = tokenCount;
			}
			return false;
		}
	}

	/** {@inheritDoc} */
    public synchronized double acquireTokenBestEffort(double upperLimitCount) {
		if (waitObject != null) {
			return 0;
		}
		if (tokens >= upperLimitCount) {
			tokens -= upperLimitCount;
			return upperLimitCount;
		} else {
			double result = tokens;
			tokens = 0;
			return result;
		}
	}

	/** {@inheritDoc} */
    public long getCapacity() {
		return capacity;
	}

	/** {@inheritDoc} */
    public double getSpeed() {
		return speed;
	}

	/** {@inheritDoc} */
    public synchronized void reset() {
		waitObject = null;
		tokens = 0;
	}

	/**
     * Setter for capacity
     *
     * @param capacity  New capacity
     */
    void setCapacity(long capacity) {
		this.capacity = capacity;
	}

	/**
     * Setter for speed
     *
     * @param speed  New speed
     */
    void setSpeed(double speed) {
		this.speed = speed;
	}

	/**
	 * Add some tokens to this bucket.
	 * 
	 * @param token        Token to add
	 */
	synchronized void addToken(double token) {
		if (tokens + token > capacity) {
			tokens = capacity;
		} else {
			tokens += token;
		}
		if (waitObject != null && tokens >= waitObject.tokenCount) {
			ITokenBucketCallback callback = waitObject.callback;
			double tokenCount = waitObject.tokenCount;
			waitObject = null;
			callback.available(this, tokenCount);
		}
	}

    /**
     * Wait object with token bucket callback and token count
     */
    private class WaitObject {
        /**
         * Token item bucket callback
         */
        private ITokenBucketCallback callback;
        /**
         * Token count
         */
		private double tokenCount;
	}
}
