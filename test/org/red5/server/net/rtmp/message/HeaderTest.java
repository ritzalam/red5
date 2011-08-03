package org.red5.server.net.rtmp.message;

import java.util.concurrent.atomic.AtomicInteger;

import net.sourceforge.groboutils.junit.v1.MultiThreadedTestRunner;
import net.sourceforge.groboutils.junit.v1.TestRunnable;

import org.junit.Assert;
import org.junit.Test;

public class HeaderTest {

	private final static AtomicInteger timer = new AtomicInteger(1);

	@Test
	public void testLifecycle() throws Throwable {
		int threads = 500;		
		TestRunnable[] trs = new TestRunnable[threads];
		for (int t = 0; t < threads; t++) {
			trs[t] = new HeaderWorker();
		}
		Runtime rt = Runtime.getRuntime();
		long startFreeMem = rt.freeMemory();
		System.out.printf("Free mem: %s\n", startFreeMem);
		MultiThreadedTestRunner mttr = new MultiThreadedTestRunner(trs);
		long start = System.nanoTime();
		mttr.runTestRunnables();
		System.out.printf("Runtime: %s ns\n", (System.nanoTime() - start));
		for (TestRunnable r : trs) {
			Header hdr = ((HeaderWorker) r).getHeader();
			Assert.assertTrue(hdr == null);
		}		
		System.gc();
		Thread.sleep(1000);
		System.out.printf("Free mem diff at end: %s\n", Math.abs(startFreeMem - rt.freeMemory()));
	}
	
	@Test
	public void testExtendedTimestamp() throws Throwable {
		int threads = 10;		
		TestRunnable[] trs = new TestRunnable[threads];
		for (int t = 0; t < threads; t++) {
			trs[t] = new HeaderWorker();
		}
		// update the timer to extended time
		timer.set(16777215); //16777215, or 4hrs 39 minutes 37.215 seconds
		MultiThreadedTestRunner mttr = new MultiThreadedTestRunner(trs);
		long start = System.nanoTime();
		mttr.runTestRunnables();
		System.out.printf("Runtime: %s ns\n", (System.nanoTime() - start));
		for (TestRunnable r : trs) {
			Header hdr = ((HeaderWorker) r).getHeader();
			Assert.assertTrue(hdr == null);
		}		
		System.gc();
		Thread.sleep(1000);
	}	
	
	private Header newHeader() {
		Header header = new Header();
		header.setChannelId(1);
		header.setDataType(Header.TYPE_AUDIO_DATA);
		header.setSize(16);
		header.setStreamId(1);
		header.setTimer(timer.incrementAndGet());
		return header;
	}
	
	@Test
	public void testReadExternal() {
		// TODO
	}

	@Test
	public void testWriteExternal() {
		// TODO
	}

	private class HeaderWorker extends TestRunnable {
		Header header;
		
		public void runTest() throws Throwable {
			header = newHeader();
			header.toString();
			header = null;
		}

		public Header getHeader() {
			return header;
		}
		
	}	
	
}
