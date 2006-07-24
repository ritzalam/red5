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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
	
	private long interval = 10;
	private long defaultCapacity = 1024 * 100;
	
	private Map<IFlowControllable, FcData> fcMap =
		new HashMap<IFlowControllable, FcData>();
	private List<FcData>[] fcListArray;
	
	private Timer timer;
	private Thread cbThread = new Thread(new CallbackRunnable());
	private BlockingQueue<FcData> wakeUpQueue = new LinkedBlockingQueue<FcData>();
	
	private ReadWriteLock lock = new ReentrantReadWriteLock();
	
	public BalancedFlowControlService() {
		fcListArray = (List<FcData>[]) Array.newInstance(List.class, maxDepth);
		for (int i = 0; i < maxDepth; i++) {
			fcListArray[i] = new ArrayList<FcData>();
		}
	}
	
	public void init() {
		timer = new Timer("FlowControlService", true);
		timer.scheduleAtFixedRate(this, interval, interval);
		cbThread.setName("Callback Thread");
		cbThread.setDaemon(true);
		cbThread.start();
	}
	
	@Override
	public void run() {
		// re-sort the fc list
		// use write lock to protect the structural modification
		lock.writeLock().lock();
		try {
			for (int i = 0; i < maxDepth; i++) {
				Collections.sort(fcListArray[i]);
			}
		} finally {
			lock.writeLock().unlock();
		}
		// distribute tokens
		// no structural modification, use read lock
		lock.readLock().lock();
		try {
			for (int i = 0; i < maxDepth; i++) {
				List<FcData> fcList = fcListArray[i];
				for (FcData fcData : fcList) {
					if (fcData.hasBandwidthResource()) {
						boolean gotToken = false;
						for (int channelId = 0; channelId < fcData.bwResources.length; channelId++) {
							BandwidthResource thisResource = fcData.bwResources[channelId];
							double tokenCount = fcData.bwResources[channelId].speed * interval;
							BandwidthResource parentResource = getParentBandwidthResource(fcData, channelId);
							if (parentResource == null) {
								thisResource.addToken(tokenCount);
							} else {
								if (parentResource.acquireToken(tokenCount)) {
									thisResource.addToken(tokenCount);
									gotToken = true;
								}
							}
						}
						if (gotToken) {
							wakeUpCallback(fcData);
						} else {
							fcData.hungry++;
						}
					} else {
						// no bw resource available, try to wake up callbacks
						wakeUpCallback(fcData);
					}
				}
			}
		} finally {
			lock.readLock().unlock();
		}
	}

	public void releaseFlowControllable(IFlowControllable fc) {
		int depth = getFCDepth(fc);
		lock.writeLock().lock();
		try {
			FcData data = fcMap.remove(fc);
			if (data != null) {
				fcListArray[depth].remove(data);
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	public void updateBWConfigure(IFlowControllable fc) {
		lock.readLock().lock();
		try {
			FcData data = getFcData(fc);
			// escalate to write lock
			lock.readLock().unlock();
			lock.writeLock().lock();
			try {
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
			} finally {
				lock.readLock().lock();
				lock.writeLock().unlock();
			}
		} finally {
			lock.readLock().unlock();
		}
	}

	public void resetTokenBuckets(IFlowControllable fc) {
		getAudioTokenBucket(fc).reset();
		getVideoTokenBucket(fc).reset();
	}

	public ITokenBucket getAudioTokenBucket(IFlowControllable fc) {
		lock.readLock().lock();
		try {
			return getFcData(fc).audioBucket;
		} finally {
			lock.readLock().unlock();
		}
	}

	public ITokenBucket getVideoTokenBucket(IFlowControllable fc) {
		lock.readLock().lock();
		try {
			return getFcData(fc).videoBucket;
		} finally {
			lock.readLock().unlock();
		}
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
	
	/**
	 * Get bw resource from the parent tree nodes.
	 * Read-lock should be held when calling into this method.
	 * @param fcData
	 * @param channelId
	 * @return
	 */
	private BandwidthResource getParentBandwidthResource(FcData fcData, int channelId) {
		IFlowControllable parentFC = fcData.fc.getParentFlowControllable();
		while (parentFC != null) {
			FcData data = getFcData(parentFC);
			if (data.hasBandwidthResource()) {
				while (channelId >= data.bwResources.length) {
					channelId--;
				}
				return data.bwResources[channelId];
			}
			parentFC = parentFC.getParentFlowControllable();
		}
		return null;
	}
	
	/**
	 * Register an fc.
	 * Write-lock should be held when calling into this method.
	 * @param fc
	 */
	private void registerFlowControllable(IFlowControllable fc) {
		int depth = getFCDepth(fc);
		FcData data = new FcData();
		data.fc = fc;
		data.videoBucket = new Bucket(data, 0);
		data.audioBucket = new Bucket(data, 1);
		fcListArray[depth].add(data);
		fcMap.put(fc, data);
		updateBWConfigure(fc);
	}
	
	/**
	 * Get the FC depth in the tree.
	 * @param fc
	 * @return
	 */
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
	 * Put a request object to sleep state.
	 * @param fcData
	 * @param reqObj
	 * @param channelId
	 */
	private void putToSleep(FcData fcData, RequestObject reqObj) {
		lock.writeLock().lock();
		try {
			fcData.waitingList.add(reqObj);
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	/**
	 * Search nodes (including itself) along the parent path for
	 * available bandwidth resource.
	 * Read-lock should be held when calling into this method.
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
	
	private void wakeUpCallback(FcData fcData) {
		try {
			wakeUpQueue.put(fcData);
		} catch (InterruptedException e) {}
	}
	
	/**
	 * Get the fcData from the fc map.
	 * Readlock should be held when calling into this method.
	 * @param fc
	 * @return
	 */
	private FcData getFcData(IFlowControllable fc) {
		FcData data = fcMap.get(fc);
		if (data == null) {
			// escalate the lock
			lock.readLock().unlock();
			lock.writeLock().lock();
			try {
				registerFlowControllable(fc);
			} finally {
				lock.readLock().lock();
				lock.writeLock().unlock();
			}
			data = fcMap.get(fc);
		}
		return data;
	}
	
	/**
	 * Bit per second to Byte per millisecond.
	 * @param bps Bit per second.
	 * @return Byte per millisecond.
	 */
	private double bps2Bpms(long bps) {
		return (double) bps / 1000.0 / 8.0;
	}
	
	private class BandwidthResource {
		private double speed;
		private long capacity;
		private double tokens = 0;
		
		public BandwidthResource(long capacity, double speed, long initial) {
			this.capacity = capacity;
			this.speed = speed;
			if (initial < 0) this.tokens = 0;
			else if (initial > capacity) this.tokens = capacity;
			else this.tokens = initial;
		}
		
		synchronized public boolean acquireToken(double token) {
			if (tokens >= token) {
				tokens -= token;
				return true;
			} else {
				return false;
			}
		}
		
		synchronized public double acquireTokenBestEffort(double upperLimit) {
			double avail = 0;
			if (tokens < upperLimit) {
				avail = tokens;
				tokens = 0;
				return avail;
			} else {
				return upperLimit;
			}
		}
		
		synchronized public void addToken(double token) {
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
		private Bucket audioBucket;
		private Bucket videoBucket;
		private SimpleBandwidthConfigure config;
		private BandwidthResource[] bwResources = new BandwidthResource[0];
		private List<RequestObject> waitingList;
		private int hungry = 0;
		
		public FcData() {
			waitingList = new ArrayList<RequestObject>();
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

		public boolean acquireToken(double tokenCount, long wait) {
			if (wait != 0) {
				// XXX not support waiting for now
				return false;
			}
			BandwidthResource resource = null;
			lock.readLock().lock();
			try {
				if (isReset) return false;
				resource = getResource();
			} finally {
				lock.readLock().unlock();
			}
			if (resource == null) return true;
			boolean success = resource.acquireToken(tokenCount);
			// lock here to avoid sorting on concurrent modification
			lock.readLock().lock();
			try {
				if (success) {
					fcData.hungry = 0;
				} else {
					fcData.hungry++;
				}
			} finally {
				lock.readLock().unlock();
			}
			return success;
		}

		public double acquireTokenBestEffort(double upperLimitCount) {
			BandwidthResource resource = null;
			lock.readLock().lock();
			try {
				if (isReset) return 0;
				resource = getResource();
			} finally {
				lock.readLock().unlock();
			}
			if (resource == null) return upperLimitCount;
			double avail = resource.acquireTokenBestEffort(upperLimitCount);
			// lock here to avoid sorting on concurrent modification
			lock.readLock().lock();
			try {
				if (avail == 0) {
					fcData.hungry++;
				} else {
					fcData.hungry = 0;
				}
			} finally {
				lock.readLock().unlock();
			}
			return avail;
		}

		public boolean acquireTokenNonblocking(double tokenCount, ITokenBucketCallback callback) {
			BandwidthResource bwResource = null;
			lock.readLock().lock();
			try {
				if (isReset) return false;
				bwResource = getResource();
			} finally {
				lock.readLock().unlock();
			}
			if (bwResource == null) return true;
			boolean success = bwResource.acquireToken(tokenCount);
			if (!success) {
				RequestObject reqObj = new RequestObject();
				reqObj.bucket = this;
				reqObj.callback = callback;
				reqObj.tokenCount = tokenCount;
				putToSleep(fcData, reqObj);
			}
			// lock here to avoid sorting on concurrent modification
			lock.readLock().lock();
			try {
				if (success) {
					fcData.hungry = 0;
				} else {
					fcData.hungry++;
				}
			} finally {
				lock.readLock().unlock();
			}
			return success;
		}

		public long getCapacity() {
			lock.readLock().lock();
			try {
				BandwidthResource bwResource = getResource();
				if (bwResource != null) {
					return bwResource.capacity;
				} else {
					return -1; // infinite
				}
			} finally {
				lock.readLock().unlock();
			}
		}

		public double getSpeed() {
			lock.readLock().unlock();
			try {
				BandwidthResource bwResource = getResource();
				if (bwResource != null) {
					return bwResource.capacity;
				} else {
					return -1; // infinite
				}
			} finally {
				lock.readLock().unlock();
			}
		}

		public void reset() {
			lock.writeLock().lock();
			try {
				isReset = true;
				for (RequestObject obj : fcData.waitingList) {
					try {
						obj.callback.reset(obj.bucket, obj.tokenCount);
					} catch (Throwable t) {
						log.error("Exception in callback reset", t);
					}
				}
				fcData.waitingList.clear();
				isReset = false;
			} finally {
				lock.writeLock().unlock();
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
		private double tokenCount;
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
	
	private class CallbackRunnable implements Runnable {
		public void run() {
			try {
				while (true) {
					FcData data = wakeUpQueue.take();
					List<RequestObject> reqObjList = data.waitingList;
					RequestObject reqObj = null;
					lock.writeLock().lock();
					try {
						if (reqObjList.size() > 0) {
							reqObj = reqObjList.remove(0);
							for (RequestObject obj : reqObjList) {
								obj.hungry++;
							}
						}
					} finally {
						lock.writeLock().unlock();
					}
					try {
						if (reqObj != null) {
							reqObj.callback.available(reqObj.bucket, reqObj.tokenCount);
						}
					} catch (Throwable t) {
						log.error("Exception in callback", t);
					}
				}
			} catch (InterruptedException e) {}
		}
	}
}
