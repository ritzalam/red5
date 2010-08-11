package org.red5.server;

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

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import net.sourceforge.groboutils.junit.v1.MultiThreadedTestRunner;
import net.sourceforge.groboutils.junit.v1.TestRunnable;

import org.junit.Test;

public class AtomicCounterTest {

	private static int threads = 3;

	private static int callsPerThread = 1000;

	private static int setSize = (threads * callsPerThread);

	@Test
	public void testIntPrimative() throws Throwable {
		IntCounter i = new IntCounter();

		//pass that instance to the MTTR
		TestRunnable[] trs = new TestRunnable[threads];
		for (int t = 0; t < threads; t++) {
			trs[t] = new IntCountWorker(i);
		}

		MultiThreadedTestRunner mttr = new MultiThreadedTestRunner(trs);

		//kickstarts the MTTR & fires off threads
		long start = System.nanoTime();
		mttr.runTestRunnables();
		System.out.println("Runtime for int primative with synchronized: "
				+ (System.nanoTime() - start) + "ns");

		//dump our ints into a set
		Set<Integer> intList = new HashSet<Integer>(setSize);
		for (TestRunnable r : trs) {
			int[] nums = ((IntCountWorker) r).getNumbers();
			for (int num : nums) {
				//a set will not allow duplicates
				intList.add(num);
			}
		}
		//check for dupes
		assertTrue(intList.size() == setSize);

	}

	@Test
	public void testAtomicInt() throws Throwable {
		AtomicIntCounter i = new AtomicIntCounter();

		//pass that instance to the MTTR
		TestRunnable[] trs = new TestRunnable[threads];
		for (int t = 0; t < threads; t++) {
			trs[t] = new AtomicIntCountWorker(i);
		}

		MultiThreadedTestRunner mttr = new MultiThreadedTestRunner(trs);

		//kickstarts the MTTR & fires off threads
		long start = System.nanoTime();
		mttr.runTestRunnables();
		System.out.println("Runtime for Atomic: " + (System.nanoTime() - start)
				+ "ns");

		//dump our ints into a set
		Set<Integer> intList = new HashSet<Integer>(setSize);
		for (TestRunnable r : trs) {
			int[] nums = ((AtomicIntCountWorker) r).getNumbers();
			for (int num : nums) {
				//a set will not allow duplicates
				intList.add(num);
			}
		}
		//check for dupes
		assertTrue(intList.size() == setSize);

	}

	@Test
	public void testVolatileIntPrimative() throws Throwable {
		VolatileIntCounter i = new VolatileIntCounter();

		//pass that instance to the MTTR
		TestRunnable[] trs = new TestRunnable[threads];
		for (int t = 0; t < threads; t++) {
			trs[t] = new VolatileIntCountWorker(i);
		}

		MultiThreadedTestRunner mttr = new MultiThreadedTestRunner(trs);

		//kickstarts the MTTR & fires off threads
		long start = System.nanoTime();
		mttr.runTestRunnables();
		System.out.println("Runtime for volatile primative: "
				+ (System.nanoTime() - start) + "ns");

		//dump our ints into a set
		Set<Integer> intList = new HashSet<Integer>(setSize);
		for (TestRunnable r : trs) {
			int[] nums = ((VolatileIntCountWorker) r).getNumbers();
			for (int num : nums) {
				//a set will not allow duplicates
				intList.add(num);
			}
		}
		//compare sizes.. this should be the same for volatile ints since the worker now locks
		assertEquals(intList.size(), setSize);

	}

	private class IntCountWorker extends TestRunnable {
		private IntCounter counter;

		private int[] nums = new int[callsPerThread];

		public IntCountWorker(IntCounter counter) {
			this.counter = counter;
		}

		public void runTest() throws Throwable {
			for (int i = 0; i < callsPerThread; i++) {
				nums[i] = counter.next();
			}
		}

		public int[] getNumbers() {
			return nums;
		}
	}

	private class AtomicIntCountWorker extends TestRunnable {
		private AtomicIntCounter counter;

		private int[] nums = new int[callsPerThread];

		public AtomicIntCountWorker(AtomicIntCounter counter) {
			this.counter = counter;
		}

		public void runTest() throws Throwable {
			for (int i = 0; i < callsPerThread; i++) {
				nums[i] = counter.next();
			}
		}

		public int[] getNumbers() {
			return nums;
		}
	}

	private class VolatileIntCountWorker extends TestRunnable {
		private VolatileIntCounter counter;

		private int[] nums = new int[callsPerThread];

		public VolatileIntCountWorker(VolatileIntCounter counter) {
			this.counter = counter;
		}

		public void runTest() throws Throwable {
			for (int i = 0; i < callsPerThread; i++) {
				nums[i] = counter.next();
			}
		}

		public int[] getNumbers() {
			return nums;
		}
	}

	/**
	 * Simple counter using int and syncronized
	 */
	public class IntCounter {

		private int counter = 0;

		public synchronized int next() {
			return counter++;
		}

		public synchronized int getCurrent() {
			return counter;
		}
	}

	/**
	 * Simple counter using AtomicInteger
	 */
	public class AtomicIntCounter {

		private AtomicInteger counter = new AtomicInteger();

		public int next() {
			return counter.incrementAndGet();
		}

		public int getCurrent() {
			return counter.get();
		}
	}

	/**
	 * Simple counter using only a primative int and volatile
	 */
	public class VolatileIntCounter {

		// Note: volatile just ensures that you're always READING the lastest
		// copy, but ++ is still not atomic, so this MUST be synced to work.
		private volatile int counter = 0;

		public int next() {
			int retval = 0;
			synchronized(this) {
				retval = counter;
				counter++;
			}
			return retval;
		}

		public int getCurrent() {
			// always an atomic read
			return counter;
		}
	}

}
