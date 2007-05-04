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

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.api.IBWControllable;
import org.red5.server.api.IBandwidthConfigure;

/**
 * A simple implementation of bandwidth controller. The initial burst,
 * if not specified by user, is half of the property "defaultCapacity".
 * <p>
 * Following is the reference information for the future optimization
 * on threading:
 * The threads that may access this object concurrently are:
 * * Thread A that makes token request.
 * * Thread B that makes token request.
 * * Thread C that distributes tokens and call the callbacks. (Timer)
 * * Thread D that updates the bw config of a controllable.
 * * Thread E that resets a bucket.
 * * Thread F that unregisters a controllable.
 * 
 * The implementation now synchronizes on each context to make sure only
 * one thread is accessing the context object at a time.
 * 
 * @author Steven Gong (steven.gong@gmail.com)
 * @version $Id$
 */
public class SimpleBWControlService extends TimerTask
implements IBWControlService {
	private static final Log log = LogFactory.getLog(SimpleBWControlService.class);
	
	protected Map<IBWControllable, BWContext> contextMap =
		new ConcurrentHashMap<IBWControllable, BWContext>();
	protected Timer tokenDistributor;
	protected long interval;
	protected long defaultCapacity;
	
	public void init() {
		tokenDistributor = new Timer("Token Distributor", true);
		tokenDistributor.scheduleAtFixedRate(this, 0, interval);
	}

	public void run() {
		Collection<BWContext> contexts = contextMap.values();
		for (BWContext context : contexts) {
			synchronized (context) {
				if (context.bwConfig != null) {
					long t = System.currentTimeMillis();
					long delta = t - context.lastSchedule;
					context.lastSchedule = t;
					if (context.bwConfig[3] >= 0) {
						if (defaultCapacity >= context.tokenRc[3]) {
							context.tokenRc[3] += (double) (context.bwConfig[3]) * delta / 8000;
						}
					} else {
						for (int i = 0; i < 3; i++) {
							if (context.bwConfig[i] >= 0 && defaultCapacity >= context.tokenRc[i]) {
								context.tokenRc[i] += (double) (context.bwConfig[i]) * delta / 8000;
							}
						}
					}
				}
			}
		}
		for (BWContext context : contexts) {
			synchronized (context) {
				// notify all blocked requests
				context.notifyAll();
				// notify all callbacks
				invokeCallback(context);
			}
		}
	}

	public ITokenBucket getAudioBucket(IBWControlContext context) {
		if (!(context instanceof BWContext)) return null;
		BWContext c = (BWContext) context;
		return c.buckets[0];
	}

	public ITokenBucket getVideoBucket(IBWControlContext context) {
		if (!(context instanceof BWContext)) return null;
		BWContext c = (BWContext) context;
		return c.buckets[1];
	}
	
	public ITokenBucket getDataBucket(IBWControlContext context) {
		if (!(context instanceof BWContext)) return null;
		BWContext c = (BWContext) context;
		return c.buckets[2];
	}

	public IBWControlContext registerBWControllable(IBWControllable bc) {
		BWContext context = new BWContext(bc);
		long[] channelInitialBurst = null;
		if (bc.getBandwidthConfigure() != null) {
			context.bwConfig = new long[4];
			for (int i = 0; i < 4; i++) {
				context.bwConfig[i] = bc.getBandwidthConfigure().getChannelBandwidth()[i];
			}
			channelInitialBurst = bc.getBandwidthConfigure().getChannelInitialBurst();
		}
		context.buckets[0] = new Bucket(bc, 0);
		context.buckets[1] = new Bucket(bc, 1);
		context.buckets[2] = new Bucket(bc, 2);
		context.tokenRc = new double[4];
		if (context.bwConfig != null) {
			// set the initial value to token resources as "defaultCapacity/2"
			for (int i = 0; i < 4; i++) {
				if (channelInitialBurst[i] >= 0) {
					context.tokenRc[i] = channelInitialBurst[i];
				} else {
					context.tokenRc[i] = defaultCapacity / 2;
				}
			}
			context.lastSchedule = System.currentTimeMillis();
		} else {
			context.lastSchedule = -1;
		}
		contextMap.put(bc, context);
		return context;
	}

	public void resetBuckets(IBWControlContext context) {
		if (!(context instanceof BWContext)) return;
		BWContext c = (BWContext) context;
		for (int i = 0; i < 3; i++) {
			c.buckets[i].reset();
		}
	}

	public void unregisterBWControllable(IBWControlContext context) {
		resetBuckets(context);
		contextMap.remove(context.getBWControllable());
	}
	
	public IBWControlContext lookupContext(IBWControllable bc) {
		return contextMap.get(bc);
	}

	public void updateBWConfigure(IBWControlContext context) {
		if (!(context instanceof BWContext)) return;
		BWContext c = (BWContext) context;
		IBWControllable bc = c.getBWControllable();
		synchronized (c) {
			if (bc.getBandwidthConfigure() == null) {
				c.bwConfig = null;
				c.lastSchedule = -1;
			} else {
				long[] oldConfig = c.bwConfig;
				c.bwConfig = new long[4];
				for (int i = 0; i < 4; i++) {
					c.bwConfig[i] = bc.getBandwidthConfigure().getChannelBandwidth()[i];
				}
				if (oldConfig == null) {
					// initialize the last schedule timestamp if necessary
					c.lastSchedule = System.currentTimeMillis();
					long[] channelInitialBurst = bc.getBandwidthConfigure().getChannelInitialBurst();
					// set the initial value to token resources as "defaultCapacity/2"
					for (int i = 0; i < 4; i++) {
						if (channelInitialBurst[i] >= 0) {
							c.tokenRc[i] = channelInitialBurst[i];
						} else {
							c.tokenRc[i] = defaultCapacity / 2;
						}
					}
				} else {
					// we have scheduled before, so migration of token is needed
					if (c.bwConfig[IBandwidthConfigure.OVERALL_CHANNEL] >=0 &&
							oldConfig[IBandwidthConfigure.OVERALL_CHANNEL] < 0) {
						c.tokenRc[IBandwidthConfigure.OVERALL_CHANNEL] +=
							c.tokenRc[IBandwidthConfigure.AUDIO_CHANNEL] +
							c.tokenRc[IBandwidthConfigure.VIDEO_CHANNEL] +
							c.tokenRc[IBandwidthConfigure.DATA_CHANNEL];
						for (int i = 0; i < 3; i++) {
							c.tokenRc[i] = 0;
						}
					} else if (c.bwConfig[IBandwidthConfigure.OVERALL_CHANNEL] < 0 &&
							oldConfig[IBandwidthConfigure.OVERALL_CHANNEL] >= 0) {
						for (int i = 0; i < 3; i++) {
							if (c.bwConfig[i] >= 0) {
								c.tokenRc[i] += c.tokenRc[IBandwidthConfigure.OVERALL_CHANNEL];
								break;
							}
						}
						c.tokenRc[IBandwidthConfigure.OVERALL_CHANNEL] = 0;
					}
				}
			}
		}
	}
	
	public void setInterval(long interval) {
		this.interval = interval;
	}
	
	public void setDefaultCapacity(long capacity) {
		this.defaultCapacity = capacity;
	}
	
	protected boolean processRequest(TokenRequest request) {
		IBWControllable bc = request.initialBC;
		while (bc != null) {
			BWContext context = contextMap.get(bc);
			if (context == null) {
				rollbackRequest(request);
				return false;
			}
			synchronized (context) {
				if (context.bwConfig != null) {
					boolean result;
					if (request.type == TokenRequestType.BLOCKING) {
						result = processBlockingRequest(request, context);
					} else if (request.type == TokenRequestType.NONBLOCKING) {
						result = processNonblockingRequest(request, context);
					} else {
						result = processBestEffortRequest(request, context);
					}
					if (!result) {
						// for non-blocking mode, the callback is
						// recorded and will be rolled back when being reset,
						// so we don't need to do rollback here.
						if (request.type != TokenRequestType.NONBLOCKING) {
							rollbackRequest(request);
						}
						return false;
					}
				}
				TokenRequestContext requestContext = new TokenRequestContext();
				requestContext.acquiredToken = request.requestToken;
				requestContext.bc = bc;
				request.acquiredStack.push(requestContext);
			}
			bc = bc.getParentBWControllable();
		}
		// for best effort request, we need to rollback over-charged tokens
		if (request.type == TokenRequestType.BEST_EFFORT) {
			rollbackRequest(request);
		}
		return true;
	}
	
	private boolean processBlockingRequest(TokenRequest request, BWContext context) {
		context.timeToWait = request.timeout;
		do {
			if (context.bwConfig[3] >= 0) {
				if (context.tokenRc[3] >= request.requestToken) {
					context.tokenRc[3] -= request.requestToken;
					request.timeout = context.timeToWait;
					return true;
				}
			} else {
				if (context.tokenRc[request.channel] < 0) return true;
				if (context.tokenRc[request.channel] >= request.requestToken) {
					context.tokenRc[request.channel] -= request.requestToken;
					request.timeout = context.timeToWait;
					return true;
				}
			}
			long beforeWait = System.currentTimeMillis();
			try {
				context.wait(context.timeToWait);
			} catch (InterruptedException e) {
			}
			context.timeToWait -= System.currentTimeMillis() - beforeWait;
		} while (context.timeToWait > 0);
		return false;
	}
	
	private boolean processNonblockingRequest(TokenRequest request, BWContext context) {
		if (context.bwConfig[3] >= 0) {
			if (context.tokenRc[3] >= request.requestToken) {
				context.tokenRc[3] -= request.requestToken;
				return true;
			}
		} else {
			if (context.tokenRc[request.channel] < 0) return true;
			if (context.tokenRc[request.channel] >= request.requestToken) {
				context.tokenRc[request.channel] -= request.requestToken;
				return true;
			}
		}
		context.pendingRequestArray[request.channel].add(request);
		return false;
	}
	
	private boolean processBestEffortRequest(TokenRequest request, BWContext context) {
		if (context.bwConfig[3] >= 0) {
			if (context.tokenRc[3] >= request.requestToken) {
				context.tokenRc[3] -= request.requestToken;
			} else {
				request.requestToken = context.tokenRc[3];
				context.tokenRc[3] = 0;
			}
		} else {
			if (context.tokenRc[request.channel] < 0) return true;
			if (context.tokenRc[request.channel] >= request.requestToken) {
				context.tokenRc[request.channel] -= request.requestToken;
			} else {
				request.requestToken = context.tokenRc[request.channel];
				context.tokenRc[request.channel] = 0;
			}
		}
		if (request.requestToken == 0) return false;
		else return true;
	}
	
	protected void invokeCallback(BWContext context) {
		// loop through all channels in a context
		for (int i = 0; i < 3; i++) {
			List<TokenRequest> pendingList = context.pendingRequestArray[i];
			if (!pendingList.isEmpty()) {
				// loop through all pending requests in a channel
				for (TokenRequest request : pendingList) {
					IBWControllable bc = context.getBWControllable();
					while (bc != null) {
						BWContext c = contextMap.get(bc);
						if (c == null) {
							// context has been unregistered, we should ignore
							// this callback
							break;
						}
						synchronized (c) {
							if (c.bwConfig != null && !processNonblockingRequest(request, c)) {
								break;
							}
						}
						TokenRequestContext requestContext = new TokenRequestContext();
						requestContext.acquiredToken = request.requestToken;
						requestContext.bc = bc;
						request.acquiredStack.push(requestContext);
						bc = bc.getParentBWControllable();
					}
					if (bc == null) {
						// successfully got the required tokens
						try {
							request.callback.available(context.buckets[request.channel], (long) request.requestToken);
						} catch (Throwable t) {
							log.error("Error calling request's callback", t);
						}
					}
				}
				pendingList.clear();
			}
		}
	}
	
	/**
	 * Give back the acquired tokens due to failing to accomplish the requested
	 * operation or over-charged tokens in the case of best-effort request.
	 * @param request
	 */
	protected void rollbackRequest(TokenRequest request) {
		while (!request.acquiredStack.isEmpty()) {
			TokenRequestContext requestContext = request.acquiredStack.pop();
			BWContext context = contextMap.get(requestContext.bc);
			if (context != null) {
				synchronized (context) {
					if (context.bwConfig != null) {
						if (context.bwConfig[3] >= 0) {
							if (request.type == TokenRequestType.BEST_EFFORT) {
								context.tokenRc[3] += requestContext.acquiredToken - request.requestToken;
							} else {
								context.tokenRc[3] += requestContext.acquiredToken;
							}
						} else {
							if (context.bwConfig[request.channel] >= 0) {
								if (request.type == TokenRequestType.BEST_EFFORT) {
									context.tokenRc[request.channel] += requestContext.acquiredToken - request.requestToken;
								} else {
									context.tokenRc[request.channel] += requestContext.acquiredToken;
								}
							}
						}
					}
				}
			}
		}
	}
	
	private class Bucket implements ITokenBucket {
		private IBWControllable bc;
		private int channel;
		
		public Bucket(IBWControllable bc, int channel) {
			this.bc = bc;
			this.channel = channel;
		}

		public boolean acquireToken(long tokenCount, long wait) {
			if (wait < 0) return false;
			TokenRequest request = new TokenRequest();
			request.type = TokenRequestType.BLOCKING;
			request.timeout = wait;
			request.channel = channel;
			request.initialBC = bc;
			request.requestToken = tokenCount;
			return processRequest(request);
		}

		public long acquireTokenBestEffort(long upperLimitCount) {
			TokenRequest request = new TokenRequest();
			request.type = TokenRequestType.BEST_EFFORT;
			request.channel = channel;
			request.initialBC = bc;
			request.requestToken = upperLimitCount;
			if (processRequest(request)) {
				return (long) request.requestToken;
			} else {
				return 0;
			}
		}

		public boolean acquireTokenNonblocking(long tokenCount, ITokenBucketCallback callback) {
			TokenRequest request = new TokenRequest();
			request.type = TokenRequestType.NONBLOCKING;
			request.callback = callback;
			request.channel = channel;
			request.initialBC = bc;
			request.requestToken = tokenCount;
			return processRequest(request);
		}

		public long getCapacity() {
			return defaultCapacity;
		}

		public double getSpeed() {
			BWContext context = contextMap.get(bc);
			if (context.bwConfig[3] >= 0) {
				return context.bwConfig[3] * 1000 / 8;
			} else {
				if (context.bwConfig[channel] >= 0) {
					return context.bwConfig[channel] * 1000 / 8;
				} else {
					return -1;
				}
			}
		}

		public void reset() {
			// TODO wake up all blocked threads
			IBWControllable bc = this.bc;
			while (bc != null) {
				BWContext context = contextMap.get(bc);
				if (context == null) {
					break;
				}
				synchronized (context) {
					List<TokenRequest> pendingList = context.pendingRequestArray[channel];
					TokenRequest toRemove = null;
					for (TokenRequest request : pendingList) {
						if (request.initialBC == this.bc) {
							rollbackRequest(request);
							toRemove = request;
							break;
						}
					}
					if (toRemove != null) {
						pendingList.remove(toRemove);
						try {
							toRemove.callback.reset(this, (long) toRemove.requestToken);
						} catch (Throwable t) {
							log.error("Error reset request's callback", t);
						}
						break;
					}
				}
				bc = bc.getParentBWControllable();
			}
		}
		
	}
	
	protected class TokenRequest {
		TokenRequestType type;
		ITokenBucket.ITokenBucketCallback callback;
		long timeout;
		int channel;
		IBWControllable initialBC;
		double requestToken;
		Stack<TokenRequestContext> acquiredStack = new Stack<TokenRequestContext>();
	}
	
	protected class TokenRequestContext {
		IBWControllable bc;
		double acquiredToken;
	}
	
	protected enum TokenRequestType {
		BLOCKING,
		NONBLOCKING,
		BEST_EFFORT
	}
	
	protected class BWContext implements IBWControlContext {
		long[] bwConfig;
		double[] tokenRc = new double[4];
		ITokenBucket[] buckets = new ITokenBucket[3];
		List<TokenRequest>[] pendingRequestArray = null;
		long lastSchedule;
		long timeToWait;
		
		private IBWControllable controllable;
		
		public BWContext(IBWControllable controllable) {
			this.controllable = controllable;
			Arrays.fill(tokenRc, 0);
			pendingRequestArray = new List[] {new LinkedList(), new LinkedList(), new LinkedList()};
		}
		
		public IBWControllable getBWControllable() {
			return controllable;
		}
	}
}
