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

import org.red5.server.api.IFlowControllable;
import org.red5.server.stream.ITokenBucket.ITokenBucketCallback;

/**
 * A dummy flow control service that always has token available.
 * @author The Red5 Project (red5@osflash.org)
 * @author Steven Gong (steven.gong@gmail.com)
 */
public class DummyFlowControlService implements IFlowControlService {
	private ITokenBucket dummyBucket = new DummyTokenBukcet();
	
	public void init() {
	}
	
	public void setInterval(long interval) {
	}
	
	public void setDefaultCapacity(long defaultCapacity) {
	}

	public void releaseFlowControllable(IFlowControllable fc) {
	}

	public void updateBWConfigure(IFlowControllable fc) {
	}

	public void resetTokenBuckets(IFlowControllable fc) {
	}

	public ITokenBucket getAudioTokenBucket(IFlowControllable fc) {
		return dummyBucket;
	}

	public ITokenBucket getVideoTokenBucket(IFlowControllable fc) {
		return dummyBucket;
	}

	private class DummyTokenBukcet implements ITokenBucket {

		public boolean acquireToken(long tokenCount, long wait) {
			return true;
		}

		public long acquireTokenBestEffort(long upperLimitCount) {
			return upperLimitCount;
		}

		public boolean acquireTokenNonblocking(long tokenCount, ITokenBucketCallback callback) {
			return true;
		}

		public long getCapacity() {
			return 0;
		}

		public long getSpeed() {
			return 0;
		}

		public void reset() {
		}
		
	}
}
