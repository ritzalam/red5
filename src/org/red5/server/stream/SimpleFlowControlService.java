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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.api.IBandwidthConfigure;
import org.red5.server.api.IFlowControllable;
import org.red5.server.api.stream.support.SimpleBandwidthConfigure;
import org.red5.server.stream.ITokenBucket.ITokenBucketCallback;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * A simple implementation of IFlowControlService.
 * No fairly distribute tokens across child nodes and no elegantly
 * order the IFlowControllables for scheduling (won't give priority to buckets
 * that have threads waiting).
 * @author The Red5 Project (red5@osflash.org)
 * @author Steven Gong (steven.gong@gmail.com)
 */
public class SimpleFlowControlService extends TimerTask implements
		IFlowControlService, ApplicationContextAware {
	private static final Log log = LogFactory
			.getLog(SimpleFlowControlService.class);

	private long interval = 10;

	private long defaultCapacity = 1024 * 100;

	private Timer timer;

	private Map<IFlowControllable, FCData> fcsMap = new HashMap<IFlowControllable, FCData>();

	public void init() {
		timer = new Timer("FlowControlService", true);
		timer.scheduleAtFixedRate(this, interval, interval);
	}

	/** {@inheritDoc} */
    @Override
	public void run() {
		synchronized (fcsMap) {
			for (IFlowControllable fc : fcsMap.keySet()) {
				FCData data = fcsMap.get(fc);
				if (data.resources.length > 0) {
					// find the nearest ancestor that has bw resource assigned
					IFlowControllable parentFC = fc.getParentFlowControllable();
					while (parentFC != null) {
						FCData parentData = fcsMap.get(parentFC);
						if (parentData != null
								&& parentData.resources.length > 0) {
							for (int i = 0; i < data.resources.length; i++) {
								for (int j = i; j >= 0; j--) {
									if (j < parentData.resources.length) {
										double tokenCount = data.resources[i]
												.getSpeed(fc)
												* interval;
										double availableTokens = parentData.resources[j]
												.acquireTokenBestEffort(fc,
														tokenCount);
										data.resources[i]
												.addToken(availableTokens);
										break;
									}
								}
							}
							break;
						}
						parentFC = parentFC.getParentFlowControllable();
					}
					if (parentFC == null) {
						for (BandwidthResource element : data.resources) {
							element.addToken(element.getSpeed(fc) * interval);
						}
					}
				}
			}
		}
	}

	/** {@inheritDoc} */
    public void releaseFlowControllable(IFlowControllable fc) {
		synchronized (fcsMap) {
			yieldResourceToAncestor(fc);
			fcsMap.remove(fc);
		}
	}

	/** {@inheritDoc} */
    public void updateBWConfigure(IFlowControllable fc) {
		synchronized (fcsMap) {
			FCData data = fcsMap.get(fc);
			if (data == null) {
				data = registerFlowControllable(fc);
			} else {
				IBandwidthConfigure newBC = fc.getBandwidthConfigure();
				long capacity = 0;
				if (newBC != null) {
					capacity = newBC.getMaxBurst() <= 0 ? defaultCapacity
							: newBC.getMaxBurst();
				}
				if (data.sbc == null && newBC == null) {
					// nothing changes
				} else if (data.sbc != null && newBC == null) {
					// configuration removed
					yieldResourceToAncestor(fc);
					data.sbc = null;
				} else if (data.sbc == null && newBC != null) {
					// configuration added
					data.sbc = new SimpleBandwidthConfigure(newBC);
					initFCDataByBWConfig(data, data.sbc);
					adoptAncestorResource(fc);
				} else {
					// configuration changes
					if (data.sbc.getOverallBandwidth() >= 0
							&& newBC.getOverallBandwidth() < 0) {
						// change to a/v bw config
						data.sbc = new SimpleBandwidthConfigure(newBC);
						BandwidthResource[] old = data.resources;
						data.resources = new BandwidthResource[2];
						data.resources[0] = old[0];
						data.resources[0].capacity = capacity;
						data.resources[0].speed = bps2Bpms(data.sbc
								.getVideoBandwidth());
						data.resources[1] = new BandwidthResource(data.sbc
								.getMaxBurst(), bps2Bpms(data.sbc
								.getAudioBandwidth()), data.sbc.getBurst());
					} else if (data.sbc.getOverallBandwidth() < 0
							&& newBC.getOverallBandwidth() >= 0) {
						// change to overall bw config
						data.sbc = new SimpleBandwidthConfigure(newBC);
						BandwidthResource[] old = data.resources;
						data.resources = new BandwidthResource[1];
						old[0].adoptAll(old[1]);
						data.resources[0] = old[0];
						data.resources[0].capacity = capacity;
						data.resources[0].speed = bps2Bpms(data.sbc
								.getOverallBandwidth());
					} else if (data.sbc.getOverallBandwidth() < 0) {
						// update a/v bw config
						data.sbc = new SimpleBandwidthConfigure(newBC);
						data.resources[0].capacity = capacity;
						data.resources[0].speed = bps2Bpms(data.sbc
								.getVideoBandwidth());
						data.resources[1].capacity = capacity;
						data.resources[1].speed = bps2Bpms(data.sbc
								.getAudioBandwidth());
					} else {
						// update overall bw config
						data.sbc = new SimpleBandwidthConfigure(newBC);
						data.resources[0].capacity = capacity;
						data.resources[0].speed = bps2Bpms(data.sbc
								.getOverallBandwidth());
					}
				}
			}
		}
	}

	/** {@inheritDoc} */
    public void resetTokenBuckets(IFlowControllable fc) {
		getAudioTokenBucket(fc).reset();
		getVideoTokenBucket(fc).reset();
	}

	/** {@inheritDoc} */
    public ITokenBucket getAudioTokenBucket(IFlowControllable fc) {
		synchronized (fcsMap) {
			FCData data = fcsMap.get(fc);
			if (data == null) {
				data = registerFlowControllable(fc);
			}
			return data.audioBucket;
		}
	}

	/** {@inheritDoc} */
    public ITokenBucket getVideoTokenBucket(IFlowControllable fc) {
		synchronized (fcsMap) {
			FCData data = fcsMap.get(fc);
			if (data == null) {
				data = registerFlowControllable(fc);
			}
			return data.videoBucket;
		}
	}

	/** {@inheritDoc} */
    public void setApplicationContext(ApplicationContext arg0)
			throws BeansException {
	}

	private FCData registerFlowControllable(IFlowControllable fc) {
		synchronized (fcsMap) {
			FCData data = new FCData();
			IBandwidthConfigure bc = fc.getBandwidthConfigure();
			data.videoBucket = new Bucket(fc, 0);
			data.audioBucket = new Bucket(fc, 1);
			initFCDataByBWConfig(data, bc);
			if (bc != null) {
				adoptAncestorResource(fc);
			}
			fcsMap.put(fc, data);
			return data;
		}
	}

	/**
	 * Bit per second to Byte per millisecond.
	 * @param bps Bit per second.
	 * @return Byte per millisecond.
	 */
	private long bps2Bpms(long bps) {
		return bps / 1000 / 8;
	}

	private void initFCDataByBWConfig(FCData data, IBandwidthConfigure bc) {
		if (bc == null) {
			data.resources = new BandwidthResource[0];
		} else {
			long capacity = bc.getMaxBurst() <= 0 ? defaultCapacity : bc
					.getMaxBurst();
			if (bc.getOverallBandwidth() >= 0) {
				// valid overall bandwidth
				data.resources = new BandwidthResource[1];
				data.resources[0] = new BandwidthResource(capacity, bps2Bpms(bc
						.getOverallBandwidth()), bc.getBurst());
			} else {
				// valid a/v bandwidth
				data.resources = new BandwidthResource[2];
				data.resources[0] = new BandwidthResource(capacity, bps2Bpms(bc
						.getVideoBandwidth()), bc.getBurst());
				data.resources[1] = new BandwidthResource(capacity, bps2Bpms(bc
						.getAudioBandwidth()), bc.getBurst());
			}
		}
	}

	/**
	 * Move related resource from ancestor to this fc.
	 * 
	 * @param fc
	 */
	private void adoptAncestorResource(IFlowControllable fc) {
		IFlowControllable currentFC, parentFC;
		FCData data, thisData;
		thisData = fcsMap.get(fc);
		currentFC = fc;
		while (true) {
			parentFC = currentFC.getParentFlowControllable();
			if (parentFC == null) {
				break;
			}
			data = fcsMap.get(parentFC);
			if (data != null) {
				for (int i = 0; i < data.resources.length; i++) {
					for (int j = i; j >= 0; j--) {
						if (j < thisData.resources.length) {
							thisData.resources[j].adopt(data.resources[i], fc);
							break;
						}
					}
				}
			}
			currentFC = parentFC;
		}
	}

	/**
	 * Give away resource to ancestor that has assigned bw resource.
	 * 
	 * @param fc
	 */
	private void yieldResourceToAncestor(IFlowControllable fc) {
		FCData thisData = fcsMap.get(fc);
		IFlowControllable parentFC = fc.getParentFlowControllable();
		while (parentFC != null) {
			FCData data = fcsMap.get(parentFC);
			if (data != null && data.resources.length > 0) {
				for (int i = 0; i < thisData.resources.length; i++) {
					for (int j = i; j >= 0; j--) {
						if (j < data.resources.length) {
							data.resources[j].adopt(thisData.resources[i], fc);
							break;
						}
					}
				}
			}
			parentFC = parentFC.getParentFlowControllable();
		}
	}

	/**
     * Setter for property 'interval'.
     *
     * @param interval Value to set for property 'interval'.
     */
    public void setInterval(long interval) {
		this.interval = interval;
	}

	/**
     * Setter for property 'defaultCapacity'.
     *
     * @param defaultCapacity Value to set for property 'defaultCapacity'.
     */
    public void setDefaultCapacity(long defaultCapacity) {
		this.defaultCapacity = defaultCapacity;
	}

	private class FCData {
		private Bucket audioBucket;

		private Bucket videoBucket;

		private BandwidthResource[] resources;

		private SimpleBandwidthConfigure sbc;
	}

	private class Bucket implements ITokenBucket {
		private int bucketNum; // video:0, audio:1

		private IFlowControllable fc;

		private boolean isReset;

		public Bucket(IFlowControllable fc, int bucketNum) {
			this.bucketNum = bucketNum;
			this.fc = fc;
		}

		/** {@inheritDoc} */
        synchronized public boolean acquireToken(double tokenCount, long wait) {
			if (isReset) {
				return false;
			}
			if (wait != 0) {
				// XXX not support waiting for now
				return false;
			}
			BandwidthResource resource = getResource();
			if (resource == null) {
				return true;
			}
			return resource.acquireToken(fc, tokenCount);
		}

		/** {@inheritDoc} */
        synchronized public double acquireTokenBestEffort(double upperLimitCount) {
			if (isReset) {
				return 0;
			}
			BandwidthResource resource = getResource();
			if (resource == null) {
				return upperLimitCount;
			}
			return resource.acquireTokenBestEffort(fc, upperLimitCount);
		}

		/** {@inheritDoc} */
        synchronized public boolean acquireTokenNonblocking(double tokenCount,
				ITokenBucketCallback callback) {
			if (isReset) {
				return false;
			}
			BandwidthResource resource = getResource();
			if (resource == null) {
				return true;
			}
			return resource.acquireToken(fc, tokenCount, callback, this);
		}

		/** {@inheritDoc} */
        public long getCapacity() {
			BandwidthResource resource = getResource();
			if (resource == null) {
				return -1; // infinite
			}
			return resource.getCapacity(fc);
		}

		/** {@inheritDoc} */
        public double getSpeed() {
			BandwidthResource resource = getResource();
			if (resource == null) {
				return -1; // infinite
			}
			return resource.getSpeed(fc);
		}

		/** {@inheritDoc} */
        synchronized public void reset() {
			BandwidthResource resource = getResource();
			if (resource != null) {
				isReset = true;
				resource.reset(fc);
				isReset = false;
			}
		}

		/**
         * Getter for property 'resource'.
         *
         * @return Value for property 'resource'.
         */
        private BandwidthResource getResource() {
			synchronized (fcsMap) {
				FCData data = fcsMap.get(fc);
				IFlowControllable currentFC = fc;
				while (data.resources.length == 0) {
					currentFC = currentFC.getParentFlowControllable();
					if (currentFC == null) {
						return null;
					}
					data = fcsMap.get(currentFC);
					if (data == null) {
						data = registerFlowControllable(currentFC);
					}
				}
				int fitBucket = bucketNum;
				while (fitBucket >= data.resources.length) {
					fitBucket--;
				}
				return data.resources[fitBucket];
			}
		}
	}

	private class BandwidthResource {
		private double speed;

		private long capacity;

		private double tokens;

		private Map<IFlowControllable, Map<ITokenBucketCallback, RequestObject>> fcWaitingMap = new HashMap<IFlowControllable, Map<ITokenBucketCallback, RequestObject>>();

		public BandwidthResource(long capacity, double speed, double initial) {
			this.capacity = capacity;
			this.speed = speed;
			if (initial < 0) {
				this.tokens = 0;
			} else if (initial > capacity) {
				this.tokens = capacity;
			} else {
				this.tokens = initial;
			}
		}

		synchronized public void addToken(double tokenCount) {
			double tmp = tokens + tokenCount;
			if (tmp > capacity) {
				tokens = capacity;
			} else {
				tokens = tmp;
			}
			IFlowControllable toReleaseFC = null;
			ITokenBucketCallback toReleaseCallback = null;
			loop: for (IFlowControllable fc : fcWaitingMap.keySet()) {
				Map<ITokenBucketCallback, RequestObject> callbackMap = fcWaitingMap
						.get(fc);
				for (ITokenBucketCallback callback : callbackMap.keySet()) {
					RequestObject reqObj = callbackMap.get(callback);
					if (reqObj.requestTokenCount <= tokens) {
						toReleaseFC = fc;
						toReleaseCallback = callback;
						break loop;
					}
				}
			}
			if (toReleaseFC != null) {
				Map<ITokenBucketCallback, RequestObject> callbackMap = fcWaitingMap
						.get(toReleaseFC);
				RequestObject reqObj = callbackMap.remove(toReleaseCallback);
				if (callbackMap.size() == 0) {
					fcWaitingMap.remove(toReleaseFC);
				}
				// finally call back
				try {
					if (log.isDebugEnabled()) {
						log.debug("Token available and calling callback: request "
							+ reqObj.requestTokenCount + ", available "
							+ tokens);
					}
					toReleaseCallback.available(reqObj.requestBucket,
							reqObj.requestTokenCount);
				} catch (Throwable t) {
					log.error("exception when calling callback", t);
				}
			}
		}

		synchronized public boolean acquireToken(IFlowControllable fc,
				double tokenCount, ITokenBucketCallback callback,
				ITokenBucket requestBucket) {
			if (tokenCount > tokens) {
				if (log.isDebugEnabled()) {
					log.debug("Token not enough: request " + tokenCount + ", available " + tokens);
				}
				Map<ITokenBucketCallback, RequestObject> callbackMap = fcWaitingMap
						.get(fc);
				if (callbackMap == null) {
					callbackMap = new HashMap<ITokenBucketCallback, RequestObject>();
					fcWaitingMap.put(fc, callbackMap);
				}
				if (!callbackMap.containsKey(callback)) {
					RequestObject reqObj = new RequestObject();
					reqObj.requestTokenCount = tokenCount;
					reqObj.requestBucket = requestBucket;
					callbackMap.put(callback, reqObj);
				}
				return false;
			} else {
				tokens -= tokenCount;
				return true;
			}
		}

		synchronized public boolean acquireToken(IFlowControllable fc,
				double tokenCount) {
			if (tokenCount > tokens) {
				return false;
			}
			tokens -= tokenCount;
			return true;
		}

		synchronized public double acquireTokenBestEffort(IFlowControllable fc,
				double upperLimitCount) {
			double available;
			if (tokens >= upperLimitCount) {
				available = upperLimitCount;
			} else {
				available = tokens;
			}
			tokens -= available;
			return available;
		}

		public long getCapacity(IFlowControllable fc) {
			return capacity;
		}

		public double getSpeed(IFlowControllable fc) {
			return speed;
		}

		synchronized public void reset(IFlowControllable fc) {
			Map<ITokenBucketCallback, RequestObject> callbackMap = fcWaitingMap
					.get(fc);
			if (callbackMap != null) {
				for (ITokenBucketCallback callback : callbackMap.keySet()) {
					RequestObject reqObj = callbackMap.get(callback);
					try {
						callback.reset(reqObj.requestBucket,
								reqObj.requestTokenCount);
					} catch (Throwable t) {
					}
				}
				fcWaitingMap.remove(callbackMap);
			}
		}

		synchronized public void adopt(BandwidthResource source,
				IFlowControllable fc) {
			Map<ITokenBucketCallback, RequestObject> srcMap = source.fcWaitingMap
					.remove(fc);
			if (srcMap != null) {
				Map<ITokenBucketCallback, RequestObject> dstMap = fcWaitingMap
						.get(fc);
				if (dstMap == null) {
					dstMap = new HashMap<ITokenBucketCallback, RequestObject>();
					fcWaitingMap.put(fc, dstMap);
				}
				dstMap.putAll(srcMap);
			}
		}

		synchronized public void adoptAll(BandwidthResource source) {
			ArrayList<IFlowControllable> list = new ArrayList<IFlowControllable>();
			list.addAll(source.fcWaitingMap.keySet());
			for (IFlowControllable fc : list) {
				adopt(source, fc);
			}
		}

		private class RequestObject {
			private double requestTokenCount;

			private ITokenBucket requestBucket;
		}
	}
}
