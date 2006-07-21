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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
 * An implementation of IFlowControlService that fairly distribute tokens
 * across child nodes and elegantly order the IFlowControllables for
 * scheduling (give priority to buckets that have been waiting longer)
 * @author The Red5 Project (red5@osflash.org)
 * @author Steven Gong (steven.gong@gmail.com)
 */
public class BalancedFlowControlService extends TimerTask implements
		IFlowControlService, ApplicationContextAware {
	private static final Log log = LogFactory.getLog(BalancedFlowControlService.class);
	
	private static final int maxDepth = 10;
	private static final int maxChannelNum = 2;
	
	private long interval = 10;
	private long defaultCapacity = 1024 * 100;
	
	private Map<IFlowControllable, FcData> fcMap =
		new HashMap<IFlowControllable, FcData>();
	private List<FcData>[] fcListArray;
	
	private Timer timer;
	
	public BalancedFlowControlService() {
		fcListArray = (List<FcData>[]) Array.newInstance(List.class, maxDepth);
		for (int i = 0; i < maxDepth; i++) {
			fcListArray[i] = new ArrayList<FcData>();
		}
	}
	
	public void init() {
		timer = new Timer("FlowControlService", true);
		timer.scheduleAtFixedRate(this, interval, interval);
	}
	
	@Override
	synchronized public void run() {
		for (int i = 0; i < maxDepth; i++) {
			List<FcData> fcList = fcListArray[i];
			for (FcData fcData : fcList) {
				if (fcData.hasBandwidthResource()) {
					for (int channelId = 0; channelId < fcData.bwResources.length; channelId++) {
						BandwidthResource thisResource = fcData.bwResources[channelId];
						long tokenCount = fcData.bwResources[channelId].speed * interval;
						boolean gotToken = true;
						BandwidthResource parentResource = getParentBandwidthResource(fcData, channelId);
						if (parentResource == null) {
							thisResource.addToken(tokenCount);
						} else {
							if (parentResource.acquireToken(tokenCount)) {
								thisResource.addToken(tokenCount);
							} else {
								gotToken = false;
							}
						}
						if (gotToken) {
							wakeFCCallback(fcData, channelId);
						}
					}
				} else {
					// no bw resource available, try to wake up all fc 
					for (int channelId = 0; channelId < maxChannelNum; channelId++) {
						BandwidthResource parentResource = getParentBandwidthResource(fcData, channelId);
						wakeFCCallback(fcData, channelId);
					}
				}
			}
		}
	}

	synchronized public void releaseFlowControllable(IFlowControllable fc) {
		int depth = getFCDepth(fc);
		FcData data = fcMap.remove(fc);
		if (data != null) {
			fcListArray[depth].remove(data);
		}
	}

	synchronized public void updateBWConfigure(IFlowControllable fc) {
		FcData data = getFcData(fc);
		IBandwidthConfigure oldConf = data.config;
		IBandwidthConfigure newConf = fc.getBandwidthConfigure();
		if (oldConf == null && newConf == null) {
			return;
		} else if (oldConf != null && newConf == null) {
			data.bwResources = new BandwidthResource[0];
			data.config = null;
		} else {
			long capacity = newConf.getMaxBurst() <= 0 ? defaultCapacity : newConf.getMaxBurst();
			if (oldConf == null) {
				data.config = new SimpleBandwidthConfigure(newConf);
				if (data.config.getOverallBandwidth() >= 0) {
					data.bwResources = new BandwidthResource[1];
					data.bwResources[0] = new BandwidthResource(
							capacity,
							bps2Bpms(data.config.getOverallBandwidth()),
							data.config.getBurst());
				} else {
					data.bwResources = new BandwidthResource[2];
					data.bwResources[0] = new BandwidthResource(
							capacity,
							bps2Bpms(data.config.getVideoBandwidth()),
							data.config.getBurst());
					data.bwResources[1] = new BandwidthResource(
							capacity,
							bps2Bpms(data.config.getAudioBandwidth()),
							data.config.getBurst());
				}
			} else {
				data.config = new SimpleBandwidthConfigure(newConf);
				if (oldConf.getOverallBandwidth() < 0 &&
						newConf.getOverallBandwidth() >= 0) {
					data.waitingListArray[0].addAll(data.waitingListArray[1]);
					Collections.sort(data.waitingListArray[0]);
					data.waitingListArray[1].clear();
					data.bwResources[1] = null;
					data.bwResources[0].capacity = capacity;
					data.bwResources[0].speed = bps2Bpms(data.config.getOverallBandwidth());
				} else if (newConf.getOverallBandwidth() < 0) {
					data.bwResources[0].capacity = capacity;
					data.bwResources[0].speed = bps2Bpms(data.config.getVideoBandwidth());
					data.bwResources[1].capacity = capacity;
					data.bwResources[1].speed = bps2Bpms(data.config.getAudioBandwidth());
				} else {
					data.bwResources[0].capacity = capacity;
					data.bwResources[0].speed = bps2Bpms(data.config.getOverallBandwidth());
				}
			}
		}
	}

	synchronized public void resetTokenBuckets(IFlowControllable fc) {
		getAudioTokenBucket(fc).reset();
		getVideoTokenBucket(fc).reset();
	}

	synchronized public ITokenBucket getAudioTokenBucket(IFlowControllable fc) {
		return getFcData(fc).audioBucket;
	}

	synchronized public ITokenBucket getVideoTokenBucket(IFlowControllable fc) {
		return getFcData(fc).videoBucket;
	}

	public void setApplicationContext(ApplicationContext arg0)
			throws BeansException {
	}
	
	public void setInterval(long interval) {
		this.interval = interval;
	}
	
	public void setDefaultCapacity(long defaultCapacity) {
		this.defaultCapacity = defaultCapacity;
	}
	
	private BandwidthResource getParentBandwidthResource(FcData fcData, int channelId) {
		IFlowControllable parentFC = fcData.fc.getParentFlowControllable();
		while (parentFC != null) {
			FcData data = getFcData(parentFC);
			if (data.bwResources.length > 0) {
				while (channelId >= data.bwResources.length) {
					channelId--;
				}
				return data.bwResources[channelId];
			}
			parentFC = parentFC.getParentFlowControllable();
		}
		return null;
	}
	
	private void registerFlowControllable(IFlowControllable fc) {
		int depth = getFCDepth(fc);
		FcData data = new FcData();
		data.fc = fc;
		data.depth = getFCDepth(fc);
		data.videoBucket = new Bucket(data, 0);
		data.audioBucket = new Bucket(data, 1);
		fcListArray[depth].add(data);
		fcMap.put(fc, data);
		updateBWConfigure(fc);
	}
	
	private int getFCDepth(IFlowControllable fc) {
		IFlowControllable currentFC = fc;
		int depth = -1;
		while (currentFC != null) {
			currentFC = currentFC.getParentFlowControllable();
			depth++;
		}
		if (depth >= maxDepth) {
			throw new IllegalArgumentException("FC depth too large");
		}
		return depth;
	}
	
	/**
	 * Put a request object to sleep state on a specific channel.
	 * @param fcData
	 * @param reqObj
	 * @param channelId
	 */
	private void putToSleep(FcData fcData, RequestObject reqObj, int channelId) {
		fcData.waitingListArray[channelId].add(reqObj);
	}
	
	/**
	 * Search nodes (including itself) along the parent path for
	 * available bandwidth resource.
	 * @param fcData
	 * @param channelId
	 * @return
	 */
	private BandwidthResource getBandwidthResource(FcData fcData, int channelId) {
		BandwidthResource bwResource = null;
		if (fcData.bwResources.length > 0) {
			while (channelId >= fcData.bwResources.length) {
				channelId--;
			}
			bwResource = fcData.bwResources[channelId];
		}
		if (bwResource == null) {
			return getParentBandwidthResource(fcData, channelId);
		} else {
			return bwResource;
		}
	}
	
	private void wakeFCCallback(FcData fcData, int channelId) {
		List<RequestObject> reqObjList = fcData.waitingListArray[channelId];
		if (reqObjList.size() > 0) {
			RequestObject reqObj = reqObjList.remove(0);
			for (RequestObject obj : reqObjList) {
				obj.hungry++;
			}
			try {
				reqObj.callback.available(reqObj.bucket, reqObj.tokenCount);
			} catch (Throwable t) {
				log.error("Exception in callback", t);
			}
		}
	}
	
	private FcData getFcData(IFlowControllable fc) {
		FcData data = fcMap.get(fc);
		if (data == null) {
			registerFlowControllable(fc);
			data = fcMap.get(fc);
		}
		return data;
	}
	
	/**
	 * Bit per second to Byte per millisecond.
	 * @param bps Bit per second.
	 * @return Byte per millisecond.
	 */
	private long bps2Bpms(long bps) {
		return bps / 1000 / 8;
	}
	
	private class BandwidthResource {
		private long speed;
		private long capacity;
		private long tokens = 0;
		
		public BandwidthResource(long capacity, long speed, long initial) {
			this.capacity = capacity;
			this.speed = speed;
			if (initial < 0) this.tokens = 0;
			else if (initial > capacity) this.tokens = capacity;
			else this.tokens = initial;
		}
		
		private boolean acquireToken(long token) {
			if (tokens >= token) {
				tokens -= token;
				return true;
			} else {
				return false;
			}
		}
		
		private long acquireTokenBestEffort(long upperLimit) {
			long avail = 0;
			if (tokens < upperLimit) {
				avail = tokens;
				tokens = 0;
				return avail;
			} else {
				return upperLimit;
			}
		}
		
		private void addToken(long token) {
			if (tokens + token > capacity) {
				tokens = capacity;
			} else {
				tokens += token;
			}
		}
	}

	/**
	 * Note: this class has a natural ordering that is inconsistent with equals.
	 */
	private class FcData implements Comparable {
		private IFlowControllable fc;
		private int depth;
		private Bucket audioBucket;
		private Bucket videoBucket;
		private SimpleBandwidthConfigure config;
		private BandwidthResource[] bwResources = new BandwidthResource[0];
		private List<RequestObject>[] waitingListArray;
		private int hungry = 0;
		
		public FcData() {
			waitingListArray = (List<RequestObject>[]) Array.newInstance(List.class, maxChannelNum);
			for (int i = 0; i < maxChannelNum; i++) {
				waitingListArray[i] = new ArrayList<RequestObject>();
			}
		}
		
		private boolean hasBandwidthResource() {
			return bwResources.length > 0;
		}

		public int compareTo(Object o) {
			FcData toCompare = (FcData) o;
			if (hungry < toCompare.hungry) return 1;
			else if (hungry == toCompare.hungry) return 0;
			else return -1;
		}
		
	}
	
	private class Bucket implements ITokenBucket {
		private int channelId; // 0 for video and 1 for audio
		private FcData fcData;
		private boolean isReset;
		
		public Bucket(FcData fcData, int channelId) {
			this.fcData = fcData;
			this.channelId = channelId;
			this.isReset = false;
		}

		public boolean acquireToken(long tokenCount, long wait) {
			synchronized (BalancedFlowControlService.this) {
				if (isReset) return false;
				if (wait != 0) {
					// XXX not support waiting for now
					return false;
				}
				BandwidthResource resource = getResource();
				if (resource == null) return true;
				boolean success = resource.acquireToken(tokenCount);
				int oldHungry = fcData.hungry;
				if (success) {
					fcData.hungry = 0;
				} else {
					fcData.hungry++;
				}
				if (oldHungry != fcData.hungry) {
					Collections.sort(fcListArray[fcData.depth]);
				}
				return success;
			}
		}

		public long acquireTokenBestEffort(long upperLimitCount) {
			synchronized (BalancedFlowControlService.this) {
				if (isReset) return 0;
				BandwidthResource resource = getResource();
				if (resource == null) return upperLimitCount;
				long avail = resource.acquireTokenBestEffort(upperLimitCount);
				int oldHungry = fcData.hungry;
				if (avail == 0) {
					fcData.hungry++;
				} else {
					fcData.hungry = 0;
				}
				if (oldHungry != fcData.hungry) {
					Collections.sort(fcListArray[fcData.depth]);
				}
				return avail;
			}
		}

		public boolean acquireTokenNonblocking(long tokenCount, ITokenBucketCallback callback) {
			synchronized (BalancedFlowControlService.this) {
				if (isReset) return false;
				BandwidthResource bwResource = getResource();
				if (bwResource != null) {
					boolean success;
					int oldHungry = fcData.hungry;
					if (bwResource.acquireToken(tokenCount)) {
						fcData.hungry = 0;
						success = true;
					} else {
						fcData.hungry++;
						RequestObject reqObj = new RequestObject();
						reqObj.bucket = this;
						reqObj.callback = callback;
						reqObj.tokenCount = tokenCount;
						putToSleep(fcData, reqObj, channelId);
						success = false;
					}
					if (oldHungry != fcData.hungry) {
						Collections.sort(fcListArray[fcData.depth]);
					}
					return success;
				} else {
					return true;
				}
			}
		}

		public long getCapacity() {
			synchronized (BalancedFlowControlService.this) {
				BandwidthResource bwResource = getResource();
				if (bwResource != null) {
					return bwResource.capacity;
				} else {
					return -1; // infinite
				}
			}
		}

		public long getSpeed() {
			synchronized (BalancedFlowControlService.this) {
				BandwidthResource bwResource = getResource();
				if (bwResource != null) {
					return bwResource.capacity;
				} else {
					return -1; // infinite
				}
			}
		}

		public void reset() {
			synchronized (BalancedFlowControlService.this) {
				isReset = true;
				for (RequestObject obj : fcData.waitingListArray[channelId]) {
					try {
						obj.callback.reset(obj.bucket, obj.tokenCount);
					} catch (Throwable t) {
						log.error("Exception in callback reset", t);
					}
				}
				fcData.waitingListArray[channelId].clear();
				isReset = false;
			}
		}
		
		private BandwidthResource getResource() {
			return getBandwidthResource(fcData, channelId);
		}
		
	}
	
	/**
	 * Note: this class has a natural ordering that is inconsistent with equals.
	 */
	private class RequestObject implements Comparable {
		private long tokenCount;
		private ITokenBucket bucket;
		private ITokenBucketCallback callback;
		private int hungry = 0;
		
		public int compareTo(Object o) {
			RequestObject toCompare = (RequestObject) o;
			if (hungry < toCompare.hungry) return 1;
			else if (hungry == toCompare.hungry) return 0;
			else return -1;
		}
	}
}
