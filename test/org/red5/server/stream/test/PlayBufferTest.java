package org.red5.server.stream.test;

/**
 * @author m.j.milicevic <marijan at info.nl>
 * @version 1.0
 */

import junit.framework.JUnit4TestAdapter;
import junit.framework.Test;
import junit.framework.TestCase;

import org.apache.mina.common.ByteBuffer;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.stream.PlayBuffer;
import org.red5.server.stream.message.RTMPMessage;

/**
 * TODO: extend testcase
 */
public class PlayBufferTest extends TestCase {
	PlayBuffer playBuffer;

	private RTMPMessage rtmpMessage;

	/** {@inheritDoc} */
    @Override
	protected void setUp() throws Exception {
		super.setUp();
		VideoData message = new VideoData(ByteBuffer.allocate(100));
		playBuffer = new PlayBuffer(1000);
		rtmpMessage = new RTMPMessage();
		rtmpMessage.setBody(message);
	}

	public void testPlayBuffer() {
		assertTrue("player buffer should be initialized", playBuffer != null);
	}

	public void testPutMessage() throws Exception {
		enqueue();
		RTMPMessage peek_message = playBuffer.peekMessage();
		assertNotNull("message shouldn't be null", peek_message);
		assertTrue(peek_message.equals(rtmpMessage));
		dequeue();

	}

	public void testTakeMessage() throws Exception {
		enqueue();
		assertTrue(playBuffer.takeMessage().equals(rtmpMessage));
		dequeue();
	}

	public void testPeekMessage() throws Exception {
		enqueue();
		assertTrue(playBuffer.peekMessage().equals(rtmpMessage));
		dequeue();
	}

	public void testClear() {
		enqueue();
		playBuffer.clear();
		assertTrue(playBuffer.getMessageCount() == 0);
	}

	public static Test suite() {
		return new JUnit4TestAdapter(PlayBufferTest.class);
	}

	/**
	 * enqueue with messages
	 */
	private void enqueue() {
		boolean success = playBuffer.putMessage(rtmpMessage);
		assertTrue("message successfully put into play buffer", success);
	}

	private void dequeue() throws Exception {
		setUp();
	}

}