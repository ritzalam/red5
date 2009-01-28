package org.red5.server.api.stream.support;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 *
 * Copyright (c) 2006-2009 by respective authors (see below). All rights reserved.
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

import static org.junit.Assert.fail;
import net.sourceforge.groboutils.junit.v1.MultiThreadedTestRunner;
import net.sourceforge.groboutils.junit.v1.TestRunnable;

import org.junit.Test;
import org.red5.server.stream.NoSyncServerStream;
import org.red5.server.stream.ServerStream;

public class StreamUtilsTest {

	/* ------- workers ------- */
	private class SynchedWorker extends TestRunnable {
		private ServerStream stream;

		public SynchedWorker(ServerStream stream) {
			this.stream = stream;
		}

		public void runTest() throws Throwable {
			for (int i = 0; i < callsPerThread; i++) {
				// set item
				stream.addItem(new SimplePlayItem());
				// get item
				stream.getItem(1);
				// next item
				stream.nextItem();
				// stop
				stream.stop();
			}
		}

	}

	private class UnSynchedWorker extends TestRunnable {
		private NoSyncServerStream stream;

		public UnSynchedWorker(NoSyncServerStream stream) {
			this.stream = stream;
		}

		public void runTest() throws Throwable {
			for (int i = 0; i < callsPerThread; i++) {
				// set item
				stream.addItem(new SimplePlayItem());
				// get item
				stream.getItem(1);
				// next item
				stream.nextItem();
				// stop
				stream.stop();
			}
		}

	}

	private static int callsPerThread = 1000;

	private static int threads = 3;

	@Test
	public void testCreateServerStream() {
		System.out.println("Not yet implemented"); // TODO
	}

	@Test
	public void testForDeadlock() {
		// test synchronized
		String name = "synchronized";
		ServerStream stream = new ServerStream();
		stream.setName(name);
		stream.setPublishedName(name);

		// pass that instance to the MTTR
		TestRunnable[] synced = new TestRunnable[threads];
		for (int t = 0; t < threads; t++) {
			synced[t] = new SynchedWorker(stream);
		}

		MultiThreadedTestRunner syncedrunner = new MultiThreadedTestRunner(
				synced);

		// kickstarts the MTTR & fires off threads
		long start = System.nanoTime();
		try {
			syncedrunner.runTestRunnables();
		} catch (Throwable e) {
			e.printStackTrace();
			fail();
		}
		System.out.println("Runtime for synced runner: "
				+ (System.nanoTime() - start) + "ns");

		// test unsynchronized
		name = "non-synchronized";
		NoSyncServerStream nstream = new NoSyncServerStream();
		nstream.setName(name);
		nstream.setPublishedName(name);

		// pass that instance to the MTTR
		TestRunnable[] unsynced = new TestRunnable[threads];
		for (int t = 0; t < threads; t++) {
			unsynced[t] = new UnSynchedWorker(nstream);
		}

		MultiThreadedTestRunner unsyncedrunner = new MultiThreadedTestRunner(
				unsynced);

		// kickstarts the MTTR & fires off threads
		start = System.nanoTime();
		try {
			unsyncedrunner.runTestRunnables();
		} catch (Throwable e) {
			e.printStackTrace();
			fail();
		}
		System.out.println("Runtime for unsynced runner: "
				+ (System.nanoTime() - start) + "ns");

	}

	@Test
	public void testGetServerStream() {
		System.out.println("Not yet implemented"); // TODO
	}

}
