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

import net.sourceforge.groboutils.junit.v1.MultiThreadedTestRunner;
import net.sourceforge.groboutils.junit.v1.TestRunnable;

import org.red5.server.api.IBWControllable;
import org.red5.server.api.IBandwidthConfigure;
import org.red5.server.api.stream.support.SimpleBandwidthConfigure;
import org.red5.server.stream.ITokenBucket.ITokenBucketCallback;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

public class BandwidthControlServiceTest extends
		AbstractDependencyInjectionSpringContextTests {
	class Client extends TestRunnable {
		private boolean[][] availArray;
		private int clientNum;
		private IBandwidthConfigure config;
		private Node parent;
		private long timeout;
		private long tokenCount;

		Client(int clientNum, Node parent, IBandwidthConfigure config, long tokenCount, boolean[][] availArray, long timeout) {
			this.clientNum = clientNum;
			this.parent = parent;
			this.config = config;
			this.tokenCount = tokenCount;
			this.availArray = availArray;
			for (int channel = 0; channel < 3; channel++) {
				for (int i = 0; i < availArray[channel].length; i++) {
					availArray[channel][i] = false;
				}
			}
			this.timeout = timeout;
		}

		@Override
		public void runTest() throws Throwable {
			long before = System.currentTimeMillis();
			Node node = new Node();
			node.setParentBWControllable(parent);
			node.setBandwidthConfigure(config);
			IBWControlService service =
				(IBWControlService) applicationContext.getBean(IBWControlService.KEY);
			IBWControlContext context = service.registerBWControllable(node);
			ITokenBucket[] buckets = new ITokenBucket[3];
			buckets[0] = service.getAudioBucket(context);
			buckets[1] = service.getVideoBucket(context);
			buckets[2] = service.getDataBucket(context);
			Loop: for (int channel = 0; channel < 3; channel++) {
				for (int i = 0; i < availArray[channel].length; i++) {
					ITokenBucketCallback callback = new ClientCallback(clientNum, channel, i, availArray);
					boolean result = buckets[channel].acquireTokenNonblocking(tokenCount, callback);
					if (result) availArray[channel][i] = true;
					while (!result && !availArray[channel][i]) {
						if (timeout >= 0 && System.currentTimeMillis() - before > timeout) {
							break Loop;
						}
						Thread.sleep(10);
					}
				}
			}
			service.unregisterBWControllable(context);
		}
	}
	class ClientCallback implements ITokenBucketCallback {
		boolean[][] availArray;
		int channel;
		int clientNum;
		int requestNum;

		ClientCallback(int clientNum, int channel, int requestNum, boolean[][] availArray) {
			this.clientNum = clientNum;
			this.channel = channel;
			this.requestNum = requestNum;
			this.availArray = availArray;
		}

		public void available(ITokenBucket bucket, long tokenCount) {
			availArray[channel][requestNum] = true;
		}

		public void reset(ITokenBucket bucket, long tokenCount) {
			System.err.println("Client " + clientNum + " on Channel " + channel + " reset on " + requestNum + "th request");
		}

	}

	class Node implements IBWControllable {
		IBandwidthConfigure config;
		IBWControllable parent;

		public IBandwidthConfigure getBandwidthConfigure() {
			return config;
		}

		public IBWControllable getParentBWControllable() {
			return parent;
		}

		public void setBandwidthConfigure(IBandwidthConfigure config) {
			this.config = config;
		}

		public void setParentBWControllable(IBWControllable parent) {
			this.parent = parent;
		}

	}

	class TokenBucketCallback implements ITokenBucketCallback {
		public void available(ITokenBucket bucket, long tokenCount) {
			isAvailable = true;
		}

		public void reset(ITokenBucket bucket, long tokenCount) {
			isReset = true;
		}
	}

	private boolean isAvailable;

	private boolean isReset;

	@Override
	protected String[] getConfigLocations() {
		return new String[] {"org/red5/server/stream/BandwidthControlServiceTest.xml"};
	}

	public void testMultiChildrenNoParent() throws Throwable {
		final int CLIENT_COUNT = 1000;
		final int REQUEST_COUNT = 100;

		boolean[][][] avail = new boolean[CLIENT_COUNT][3][REQUEST_COUNT];
		TestRunnable[] runnables = new TestRunnable[CLIENT_COUNT];
		for (int i = 0; i < CLIENT_COUNT; i++) {
			IBandwidthConfigure c = new SimpleBandwidthConfigure();
			long[] b = c.getChannelBandwidth();
			b[3] = 100 * 1024 * 1024;
			long[] initial = c.getChannelInitialBurst();
			initial[3] = 0;
			runnables[i] = new Client(i, null, c, 1024 * 64, avail[i], 1700);
		}
		MultiThreadedTestRunner mttr = new MultiThreadedTestRunner(runnables);
		mttr.runTestRunnables();
		Thread.sleep(2000);
		// check all avails
		for (int i = 0; i < CLIENT_COUNT; i++) {
			for (int channel = 0; channel < 3; channel++) {
				for (int j = 0; j < REQUEST_COUNT; j++) {
					assertTrue("Client No." + i + " on Channel " + channel + " fails on " + j + "th request", avail[i][channel][j]);
				}
			}
		}
	}

	public void testMultiChildrenWithParent() throws Throwable {
		final int CLIENT_COUNT = 50;
		final int REQUEST_COUNT = 100;
		Node parent = new Node();
		parent.setParentBWControllable(null);
		IBandwidthConfigure config = new SimpleBandwidthConfigure();
		long[] channelBandwidth = config.getChannelBandwidth();
		channelBandwidth[3] = 100 * 1024 * 1024 * CLIENT_COUNT;
		long[] initialBurst = config.getChannelInitialBurst();
		initialBurst[3] = 100 * 1024 * 1024;
		parent.setBandwidthConfigure(config);
		IBWControlService service =
			(IBWControlService) applicationContext.getBean(IBWControlService.KEY);
		IBWControlContext context = service.registerBWControllable(parent);

		boolean[][][] avail = new boolean[CLIENT_COUNT][3][REQUEST_COUNT];
		TestRunnable[] runnables = new TestRunnable[CLIENT_COUNT];
		for (int i = 0; i < CLIENT_COUNT; i++) {
			IBandwidthConfigure c = new SimpleBandwidthConfigure();
			long[] b = c.getChannelBandwidth();
			b[3] = 100 * 1024 * 1024;
			long[] initial = c.getChannelInitialBurst();
			initial[3] = 0;
			runnables[i] = new Client(i, parent, c, 1024 * 64, avail[i], 1700);
		}
		MultiThreadedTestRunner mttr = new MultiThreadedTestRunner(runnables);
		mttr.runTestRunnables();
		Thread.sleep(2000);
		// check all avails
		for (int i = 0; i < CLIENT_COUNT; i++) {
			for (int channel = 0; channel < 3; channel++) {
				for (int j = 0; j < REQUEST_COUNT; j++) {
					assertTrue("Client No." + i + " on Channel " + channel + " fails on " + j + "th request", avail[i][channel][j]);
				}
			}
		}
		service.unregisterBWControllable(context);
	}

	public void testSingleNodeNoParent() throws Exception {
		IBWControlService bwController =
			(IBWControlService) applicationContext.getBean(IBWControlService.KEY);
		ITokenBucketCallback callback = new TokenBucketCallback();
		Node node = new Node();
		node.setParentBWControllable(null);
		node.setBandwidthConfigure(null);
		IBWControlContext context = bwController.registerBWControllable(node);
		ITokenBucket audioBucket = bwController.getAudioBucket(context);
		ITokenBucket videoBucket = bwController.getVideoBucket(context);
		ITokenBucket dataBucket = bwController.getDataBucket(context);
		// no bandwidth configure, all should succeed
		assertTrue(audioBucket.acquireToken(0, 0));
		assertTrue(audioBucket.acquireToken(1024, 0));
		assertTrue(audioBucket.acquireToken(1024 * 1024, 0));
		assertTrue(videoBucket.acquireToken(0, 0));
		assertTrue(videoBucket.acquireToken(1024, 0));
		assertTrue(videoBucket.acquireToken(1024 * 1024, 0));
		assertTrue(dataBucket.acquireToken(0, 0));
		assertTrue(dataBucket.acquireToken(1024, 0));
		assertTrue(dataBucket.acquireToken(1024 * 1024, 0));

		// use overall bandwidth
		SimpleBandwidthConfigure config = new SimpleBandwidthConfigure();
		long[] channelBandwidth = config.getChannelBandwidth();
		long[] channelInitialBurst = config.getChannelInitialBurst();
		channelBandwidth[3] = 1024 * 1024; // 1Mbps overall bandwidth
		channelInitialBurst[3] = 1024 * 1024;
		node.setBandwidthConfigure(config);
		bwController.updateBWConfigure(context);
		// acquire the initial 1024 * 1024 tokens, should succeed
		assertTrue(audioBucket.acquireToken(1024 * 1024, 0));
		// acquire 1024 * 1024 tokens w/o wait, should fail
		assertFalse(audioBucket.acquireToken(1024 * 1024, 0));
		assertFalse(videoBucket.acquireToken(1024 * 1024, 0));
		assertFalse(dataBucket.acquireToken(1024 * 1024, 0));
		// wait 1.1s for 128KB, should succeed
		assertTrue(audioBucket.acquireToken(1024 * 1024 / 8, 1100));
		// wait 0.5s for 128KB, should fail
		assertFalse(videoBucket.acquireToken(1024 * 1024 / 8, 500));
		// wait another 0.6s for 128KB, should succeed
		assertTrue(dataBucket.acquireToken(1024 * 1024 / 8, 600));
		isAvailable = false;
		isReset = false;
		// expect false result and wait for callback
		assertFalse(audioBucket.acquireTokenNonblocking(1024 * 1024 / 8, callback));
		Thread.sleep(500);
		assertFalse(isAvailable);
		assertFalse(isReset);
		Thread.sleep(600);
		assertTrue(isAvailable);
		assertFalse(isReset);

		// wait 500 then best-effort acquire 128KB
		// the value should be larger than 0 and less than 128KB
		audioBucket.acquireTokenBestEffort(1024 * 1024);
		Thread.sleep(500);
		long acquired = audioBucket.acquireTokenBestEffort(1024 * 1024 / 8);
		assertTrue("Acquired " + acquired, acquired > 0 && acquired < 1024 * 1024 / 8);

		isAvailable = false;
		isReset = false;
		// expect false result and wait for callback
		assertFalse(videoBucket.acquireTokenNonblocking(1024 * 1024 / 8, callback));
		bwController.resetBuckets(context);
		Thread.sleep(500);
		assertFalse(isAvailable);
		assertTrue(isReset);
		Thread.sleep(600);
		assertFalse(isAvailable);
		assertTrue(isReset);
		isAvailable = false;
		isReset = false;

		isAvailable = false;
		isReset = false;
		// expect false result and wait for callback
		assertFalse(dataBucket.acquireTokenNonblocking(1024 * 1024, callback));
		bwController.unregisterBWControllable(context);
		assertFalse(isAvailable);
		assertTrue(isReset);

		channelBandwidth[3] = -1;
		channelBandwidth[0] = 1024 * 1024; // 1Mbps
		channelBandwidth[1] = 1024 * 512; // 512Kbps
		channelBandwidth[2] = 1024 * 256; // 256Kbps
		channelInitialBurst[0] = channelInitialBurst[1] = channelInitialBurst[2] = 0;
		context = bwController.registerBWControllable(node);
		audioBucket = bwController.getAudioBucket(context);
		videoBucket = bwController.getVideoBucket(context);
		dataBucket = bwController.getDataBucket(context);
		assertFalse(audioBucket.acquireToken(1024 * 1024 / 8, 500));
		assertTrue(audioBucket.acquireToken(1024 * 1024 / 8, 600));

		Thread.sleep(500);
		acquired = videoBucket.acquireTokenBestEffort(1024 * 1024 / 8);
		assertTrue("Acquired " + acquired, acquired > 0 && acquired < 1024 * 1024 / 8);
		Thread.sleep(500);

		isAvailable = false;
		isReset = false;
		// expect false result and wait for callback
		assertFalse(dataBucket.acquireTokenNonblocking(1024 * 1024 / 8, callback));
		Thread.sleep(1000);
		assertFalse(isAvailable);
		assertFalse(isReset);
		Thread.sleep(1100);
		assertTrue(isAvailable);
		assertFalse(isReset);
		bwController.unregisterBWControllable(context);
		assertTrue(isAvailable);
		assertFalse(isReset);
	}

	public void testSingleNodeWithParent() throws Exception {
		IBWControlService bwController =
			(IBWControlService) applicationContext.getBean(IBWControlService.KEY);
		ITokenBucketCallback callback = new TokenBucketCallback();

		Node parent = new Node();
		parent.setParentBWControllable(null);
		parent.setBandwidthConfigure(null);
		IBWControlContext parentContext = bwController.registerBWControllable(parent);

		Node node = new Node();
		node.setParentBWControllable(parent);
		node.setBandwidthConfigure(null);
		IBWControlContext context = bwController.registerBWControllable(node);
		ITokenBucket audioBucket = bwController.getAudioBucket(context);
		ITokenBucket videoBucket = bwController.getVideoBucket(context);
		ITokenBucket dataBucket = bwController.getDataBucket(context);
		// no bandwidth configure, all should succeed
		assertTrue(audioBucket.acquireToken(0, 0));
		assertTrue(audioBucket.acquireToken(1024, 0));
		assertTrue(audioBucket.acquireToken(1024 * 1024, 0));
		assertTrue(videoBucket.acquireToken(0, 0));
		assertTrue(videoBucket.acquireToken(1024, 0));
		assertTrue(videoBucket.acquireToken(1024 * 1024, 0));
		assertTrue(dataBucket.acquireToken(0, 0));
		assertTrue(dataBucket.acquireToken(1024, 0));
		assertTrue(dataBucket.acquireToken(1024 * 1024, 0));

		// use overall bandwidth
		SimpleBandwidthConfigure config = new SimpleBandwidthConfigure();
		long[] channelBandwidth = config.getChannelBandwidth();
		long[] channelInitialBurst = config.getChannelInitialBurst();
		channelBandwidth[3] = 1024 * 1024; // 1Mbps overall bandwidth
		channelInitialBurst[3] = 1024 * 1024;
		parent.setBandwidthConfigure(config);
		bwController.updateBWConfigure(parentContext);
		// acquire the initial 1024 * 1024 tokens, should succeed
		assertTrue(audioBucket.acquireToken(1024 * 1024, 0));
		// acquire 1024 * 1024 tokens w/o wait, should fail
		assertFalse(audioBucket.acquireToken(1024 * 1024, 0));
		assertFalse(videoBucket.acquireToken(1024 * 1024, 0));
		assertFalse(dataBucket.acquireToken(1024 * 1024, 0));
		// wait 1.1s for 128KB, should succeed
		assertTrue(audioBucket.acquireToken(1024 * 1024 / 8, 1100));
		// wait 0.5s for 128KB, should fail
		assertFalse(videoBucket.acquireToken(1024 * 1024 / 8, 500));
		// wait another 0.6s for 128KB, should succeed
		assertTrue(dataBucket.acquireToken(1024 * 1024 / 8, 600));
		isAvailable = false;
		isReset = false;
		// expect false result and wait for callback
		assertFalse(audioBucket.acquireTokenNonblocking(1024 * 1024 / 8, callback));
		Thread.sleep(500);
		assertFalse(isAvailable);
		assertFalse(isReset);
		Thread.sleep(600);
		assertTrue(isAvailable);
		assertFalse(isReset);

		// wait 500 then best-effort acquire 128KB
		// the value should be larger than 0 and less than 128KB
		audioBucket.acquireTokenBestEffort(1024 * 1024);
		Thread.sleep(500);
		long acquired = audioBucket.acquireTokenBestEffort(1024 * 1024 / 8);
		assertTrue("Acquired " + acquired, acquired > 0 && acquired < 1024 * 1024 / 8);

		isAvailable = false;
		isReset = false;
		// expect false result and wait for callback
		assertFalse(videoBucket.acquireTokenNonblocking(1024 * 1024 / 8, callback));
		bwController.resetBuckets(context);
		Thread.sleep(500);
		assertFalse(isAvailable);
		assertTrue(isReset);
		Thread.sleep(600);
		assertFalse(isAvailable);
		assertTrue(isReset);
		isAvailable = false;
		isReset = false;

		isAvailable = false;
		isReset = false;
		// expect false result and wait for callback
		assertFalse(dataBucket.acquireTokenNonblocking(1024 * 1024, callback));
		bwController.unregisterBWControllable(context);
		assertFalse(isAvailable);
		assertTrue(isReset);

		channelBandwidth[3] = -1;
		channelBandwidth[0] = 1024 * 1024; // 1Mbps
		channelBandwidth[1] = 1024 * 512; // 512Kbps
		channelBandwidth[2] = 1024 * 256; // 256Kbps
		channelInitialBurst[0] = channelInitialBurst[1] = channelInitialBurst[2] = 0;
		bwController.updateBWConfigure(parentContext);
		context = bwController.registerBWControllable(node);
		audioBucket = bwController.getAudioBucket(context);
		videoBucket = bwController.getVideoBucket(context);
		dataBucket = bwController.getDataBucket(context);
		assertFalse(audioBucket.acquireToken(1024 * 1024 / 8, 500));
		assertTrue(audioBucket.acquireToken(1024 * 1024 / 8, 600));

		Thread.sleep(500);
		acquired = videoBucket.acquireTokenBestEffort(1024 * 1024 / 8);
		assertTrue("Acquired " + acquired, acquired > 0 && acquired < 1024 * 1024 / 8);
		Thread.sleep(500);

		isAvailable = false;
		isReset = false;
		// expect false result and wait for callback
		assertFalse(dataBucket.acquireTokenNonblocking(1024 * 1024 / 8, callback));
		Thread.sleep(1000);
		assertFalse(isAvailable);
		assertFalse(isReset);
		Thread.sleep(1100);
		assertTrue(isAvailable);
		assertFalse(isReset);
		bwController.unregisterBWControllable(context);
		assertTrue(isAvailable);
		assertFalse(isReset);
		bwController.unregisterBWControllable(parentContext);
	}
}
