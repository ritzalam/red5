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

import java.util.HashMap;
import java.util.Map;

import org.red5.server.api.IBWControllable;

/**
 * A dummy bandwidth control service (bandwidth controller) that
 * always has token available.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Steven Gong (steven.gong@gmail.com)
 */
public class DummyBWControlService implements IBWControlService {
	private ITokenBucket dummyBucket = new DummyTokenBukcet();
	private Map<IBWControllable, IBWControlContext> contextMap =
		new HashMap<IBWControllable, IBWControlContext>();

	public ITokenBucket getAudioBucket(IBWControlContext context) {
		return dummyBucket;
	}

	public ITokenBucket getDataBucket(IBWControlContext context) {
		return dummyBucket;
	}

	public ITokenBucket getVideoBucket(IBWControlContext context) {
		return dummyBucket;
	}

	public IBWControlContext registerBWControllable(IBWControllable bc) {
		DummyBWContext context = new DummyBWContext(bc);
		contextMap.put(bc, context);
		return context;
	}

	public void resetBuckets(IBWControlContext context) {
		// do nothing
	}

	public void unregisterBWControllable(IBWControlContext context) {
		contextMap.remove(context.getBWControllable());
	}

	public void updateBWConfigure(IBWControlContext context) {
		// do nothing
	}

	public IBWControlContext lookupContext(IBWControllable bc) {
		return contextMap.get(bc);
	}

	static class DummyTokenBukcet implements ITokenBucket {

		/** {@inheritDoc} */
        public boolean acquireToken(long tokenCount, long wait) {
			return true;
		}

		/** {@inheritDoc} */
        public long acquireTokenBestEffort(long upperLimitCount) {
			return upperLimitCount;
		}

		/** {@inheritDoc} */
        public boolean acquireTokenNonblocking(long tokenCount,
				ITokenBucketCallback callback) {
			return true;
		}

		/** {@inheritDoc} */
        public long getCapacity() {
			return 0;
		}

		/** {@inheritDoc} */
        public double getSpeed() {
			return 0;
		}

		/** {@inheritDoc} */
        public void reset() {
		}

	}
	
	private class DummyBWContext implements IBWControlContext {
		private IBWControllable controllable;
		
		public DummyBWContext(IBWControllable controllable) {
			this.controllable = controllable;
		}
		
		public IBWControllable getBWControllable() {
			return controllable;
		}
	}
}
