package org.red5.server;

/*
 *   @(#) $Id: PooledByteBufferAllocator.java 391231 2006-04-04 06:21:55Z trustin $
 *
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.ByteBufferAllocator;
import org.apache.mina.util.ExpiringStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ByteBufferAllocator} which pools allocated buffers.
 * <p>
 * All buffers are allocated with the size of power of 2 (e.g. 16, 32, 64, ...)
 * This means that you cannot simply assume that the actual capacity of the
 * buffer and the capacity you requested are same.
 * </p>
 * <p>
 * This allocator releases the buffers which have not been in use for a certain
 * period. You can adjust the period by calling {@link #setTimeout(int)}. The
 * default timeout is 1 minute (60 seconds). To release these buffers
 * periodically, a daemon thread is started when a new instance of the allocator
 * is created. You can stop the thread by calling {@link #dispose()}.
 * </p>
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev: 391231 $, $Date: 2006-04-04 15:21:55 +0900 (Tue, 04 Apr 2006)
 *          $
 */
public class DebugPooledByteBufferAllocator implements ByteBufferAllocator {
	/**
	 * Logger
	 */
	protected static Logger log = LoggerFactory
			.getLogger(DebugPooledByteBufferAllocator.class);

	/**
     *
     */
	protected static ThreadLocal<String> local = new ThreadLocal<String>();

	/** Contains stack traces where buffers were allocated. */
	protected HashMap<UnexpandableByteBuffer, StackTraceElement[]> stacks = new HashMap<UnexpandableByteBuffer, StackTraceElement[]>();

	/**
	 * Save a stack trace for every buffer allocated?
	 * 
	 * Warning: This slows down the Red5 a lot!
	 * 
	 */
	protected boolean saveStacks;

	/**
	 * 
	 * @param section
	 */
	@SuppressWarnings("all")
	public static void setCodeSection(String section) {
		local.set(section);
	}

	/**
	 * 
	 * @return code section
	 */
	public static String getCodeSection() {
		return (local.get() == null) ? "unknown" : (String) local.get();
	}

	/**
     *
     */
	private static final int MINIMUM_CAPACITY = 1;

	/**
     *
     */
	private static int threadId;

	/**
     *
     */
	private int count;

	/**
     *
     */
	private final Expirer expirer;

	/**
     *
     */
	private final ExpiringStack containerStack = new ExpiringStack();

	/**
     *
     */
	private final ExpiringStack[] heapBufferStacks = new ExpiringStack[] {
			new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
			new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
			new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
			new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
			new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
			new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
			new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
			new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
			new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
			new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
			new ExpiringStack(), new ExpiringStack(), };

	/**
     *
     */
	private final ExpiringStack[] directBufferStacks = new ExpiringStack[] {
			new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
			new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
			new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
			new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
			new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
			new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
			new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
			new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
			new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
			new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
			new ExpiringStack(), new ExpiringStack(), };

	/**
	 * Timeout
	 */
	private int timeout;

	/**
     *
     */
	private boolean disposed;

	/**
	 * Creates a new instance with the default timeout.
	 */
	public DebugPooledByteBufferAllocator() {
		this(60, false);
	}

	/**
	 * 
	 * @param saveStacks whether to save stacks
	 */
	public DebugPooledByteBufferAllocator(boolean saveStacks) {
		this(60, saveStacks);
	}

	/**
	 * Creates a new instance with the specified <tt>timeout</tt>.
	 * 
	 * @param timeout timeout for allocation
	 */
	public DebugPooledByteBufferAllocator(int timeout) {
		this(timeout, false);
	}

	/**
	 * 
	 * @param timeout timeout for allocation
	 * @param saveStacks whether to save stacks
	 */
	public DebugPooledByteBufferAllocator(int timeout, boolean saveStacks) {
		this.saveStacks = saveStacks;
		setTimeout(timeout);
		expirer = new Expirer();
		expirer.start();
	}

	/**
	 * Stops the thread which releases unused buffers and make this allocator
	 * unusable from now on.
	 */
	public void dispose() {
		if (this == ByteBuffer.getAllocator()) {
			throw new IllegalStateException("This allocator is in use.");
		}

		expirer.shutdown();
		synchronized (containerStack) {
			containerStack.clear();
		}

		for (int i = directBufferStacks.length - 1; i >= 0; i--) {
			ExpiringStack stack = directBufferStacks[i];
			synchronized (stack) {
				stack.clear();
			}
		}
		for (int i = heapBufferStacks.length - 1; i >= 0; i--) {
			ExpiringStack stack = heapBufferStacks[i];
			synchronized (stack) {
				stack.clear();
			}
		}
		disposed = true;
	}

	/**
	 * Returns the timeout value of this allocator in seconds.
	 * 
	 * @return the timeout in seconds
	 */
	public int getTimeout() {
		return timeout;
	}

	/**
	 * Returns the timeout value of this allocator in milliseconds.
	 * 
	 * @return timeout in milliseconds
	 */
	public long getTimeoutMillis() {
		return timeout * 1000L;
	}

	/**
	 * Sets the timeout value of this allocator in seconds.
	 * 
	 * @param timeout <tt>0</tt> or negative value to disable timeout.
	 */
	public void setTimeout(int timeout) {
		if (timeout < 0) {
			timeout = 0;
		}
		this.timeout = timeout;
	}

	/**
	 * 
	 * @param capacity capacity of byte buffer
	 * @param direct whether to allocate direct byte buffers or not
	 * @return a byte buffer
	 */
	public ByteBuffer allocate(int capacity, boolean direct) {
		ensureNotDisposed();
		UnexpandableByteBuffer ubb = allocate0(capacity, direct);
		PooledByteBuffer buf = allocateContainer();
		buf.init(ubb, true);
		return buf;
	}

	/**
	 * 
	 * @return
	 */
	private PooledByteBuffer allocateContainer() {
		PooledByteBuffer buf;
		synchronized (containerStack) {
			buf = (PooledByteBuffer) containerStack.pop();
		}

		if (buf == null) {
			buf = new PooledByteBuffer();
		}
		return buf;
	}

	/**
     *
     */
	public void resetStacks() {
		synchronized (stacks) {
			stacks.clear();
		}
	}

	/**
     *
     */
	public void printStacks() {
		synchronized (stacks) {
			for (Entry<UnexpandableByteBuffer, StackTraceElement[]> entry : stacks
					.entrySet()) {
				System.err.println("Stack for buffer " + entry.getKey());
				StackTraceElement[] stack = entry.getValue();
				for (StackTraceElement element : stack) {
					System.err.println("  " + element);
				}
			}
		}
	}

	/**
	 * 
	 * @param capacity
	 * @param direct
	 * @return
	 */
	private UnexpandableByteBuffer allocate0(int capacity, boolean direct) {
		count++;

		ExpiringStack[] bufferStacks = direct ? directBufferStacks
				: heapBufferStacks;
		int idx = getBufferStackIndex(bufferStacks, capacity);
		ExpiringStack stack = bufferStacks[idx];

		UnexpandableByteBuffer buf;
		synchronized (stack) {
			buf = (UnexpandableByteBuffer) stack.pop();
		}

		if (buf == null) {
			java.nio.ByteBuffer nioBuf = direct ? java.nio.ByteBuffer
					.allocateDirect(MINIMUM_CAPACITY << idx)
					: java.nio.ByteBuffer.allocate(MINIMUM_CAPACITY << idx);
			buf = new UnexpandableByteBuffer(nioBuf);
		}

		buf.init();
		log.info("+++ " + count + " (" + buf.buf().capacity() + ") "
				+ getCodeSection() + " req: " + capacity);
		if (saveStacks) {
			synchronized (stacks) {
				stacks.put(buf, Thread.currentThread().getStackTrace());
			}
		}

		return buf;
	}

	/**
	 * 
	 * @param buf
	 */
	private void release0(UnexpandableByteBuffer buf) {

		count--;
		log.info("--- " + count + " (" + buf.buf().capacity() + ") "
				+ getCodeSection());
		if (saveStacks) {
			synchronized (stacks) {
				stacks.remove(buf);
			}
		}
		ExpiringStack[] bufferStacks = buf.buf().isDirect() ? directBufferStacks
				: heapBufferStacks;
		ExpiringStack stack = bufferStacks[getBufferStackIndex(bufferStacks,
				buf.buf().capacity())];

		synchronized (stack) {
			// push back
			stack.push(buf);
		}
	}

	/**
	 * 
	 * @param nioBuffer the buffer to wrap
	 * @return a wrapped buffer
	 */
	public ByteBuffer wrap(java.nio.ByteBuffer nioBuffer) {
		ensureNotDisposed();
		PooledByteBuffer buf = allocateContainer();
		buf.init(new UnexpandableByteBuffer(nioBuffer), false);
		buf.buf.init();
		buf.setPooled(false);
		return buf;
	}

	/**
	 * 
	 * @param bufferStacks
	 * @param size
	 * @return
	 */
	private int getBufferStackIndex(ExpiringStack[] bufferStacks, int size) {
		int targetSize = MINIMUM_CAPACITY;
		int stackIdx = 0;
		while (size > targetSize) {
			targetSize <<= 1;
			stackIdx++;
			if (stackIdx >= bufferStacks.length) {
				throw new IllegalArgumentException("Buffer size is too big: "
						+ size);
			}
		}

		return stackIdx;
	}

	/**
     *
     */
	private void ensureNotDisposed() {
		if (disposed) {
			throw new IllegalStateException(
					"This allocator is disposed already.");
		}
	}

	/**
     *
     */
	private class Expirer extends Thread {
		/**
         *
         */
		private boolean timeToStop;

		/**
         *
         */
		public Expirer() {
			super("PooledByteBufferExpirer-" + threadId++);
			setDaemon(true);
		}

		/**
         *
         */
		public void shutdown() {
			timeToStop = true;
			interrupt();
			while (isAlive()) {
				try {
					join();
				} catch (InterruptedException e) {
					break;
				}
			}
		}

		/**
         *
         */
		@Override
		public void run() {
			// Expire unused buffers every seconds
			while (!timeToStop) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					log.debug("InterruptedEx");
				}

				// Check if expiration is disabled.
				long timeout = getTimeoutMillis();
				if (timeout <= 0L) {
					continue;
				}

				// Expire old buffers
				long expirationTime = System.currentTimeMillis() - timeout;
				synchronized (containerStack) {
					containerStack.expireBefore(expirationTime);
				}

				for (int i = directBufferStacks.length - 1; i >= 0; i--) {
					ExpiringStack stack = directBufferStacks[i];
					synchronized (stack) {
						stack.expireBefore(expirationTime);
					}
				}

				for (int i = heapBufferStacks.length - 1; i >= 0; i--) {
					ExpiringStack stack = heapBufferStacks[i];
					synchronized (stack) {
						stack.expireBefore(expirationTime);
					}
				}
			}
		}
	}

	/**
     *
     */
	private class PooledByteBuffer extends ByteBuffer {
		/**
         *
         */
		private UnexpandableByteBuffer buf;

		/**
         *
         */
		private int refCount = 1;

		/**
         *
         */
		private boolean autoExpand;

		/**
         *
         */
		protected PooledByteBuffer() {
		}

		public synchronized void init(UnexpandableByteBuffer buf, boolean clear) {
			this.buf = buf;
			if (clear) {
				buf.buf().clear();
			}
			buf.buf().order(ByteOrder.BIG_ENDIAN);
			autoExpand = false;
			refCount = 1;
		}

		/**
         *
         */
		@Override
		public synchronized void acquire() {
			if (refCount <= 0) {
				throw new IllegalStateException("Already released buffer.");
			}

			refCount++;
		}

		/**
         *
         */
		@Override
		public void release() {
			synchronized (this) {
				if (refCount <= 0) {
					refCount = 0;
					throw new IllegalStateException(
							"Already released buffer.  You released the buffer too many times.");
				}

				refCount--;
				if (refCount > 0) {
					return;
				}
			}

			// No need to return buffers to the pool if it is disposed already.
			if (disposed) {
				return;
			}

			buf.release();

			synchronized (containerStack) {
				containerStack.push(this);
			}
		}

		@Override
		public java.nio.ByteBuffer buf() {
			return buf.buf();
		}

		@Override
		public boolean isDirect() {
			return buf.buf().isDirect();
		}

		@Override
		public boolean isReadOnly() {
			return buf.buf().isReadOnly();
		}

		@Override
		public boolean isAutoExpand() {
			return autoExpand;
		}

		/**
		 * 
		 * @param autoExpand should buffer auto-expand
		 * @return the buffer
		 */
		@Override
		public ByteBuffer setAutoExpand(boolean autoExpand) {
			this.autoExpand = autoExpand;
			return this;
		}

		/**
		 * 
		 * @return is this buffer pooled
		 */
		@Override
		public boolean isPooled() {
			return buf.isPooled();
		}

		/**
		 * 
		 * @param pooled set whether this buffer is polled
		 */
		@Override
		public void setPooled(boolean pooled) {
			buf.setPooled(pooled);
		}

		/**
		 * 
		 * @return capacity of this buffer
		 */
		@Override
		public int capacity() {
			return buf.buf().capacity();
		}

		/**
		 * 
		 * @return the position
		 */
		@Override
		public int position() {
			return buf.buf().position();
		}

		/**
		 * 
		 * @param newPosition the new position
		 * @return the byte buffer
		 */
		@Override
		public ByteBuffer position(int newPosition) {
			autoExpand(newPosition, 0);
			buf.buf().position(newPosition);
			return this;
		}

		/**
		 * 
		 * @return the limit of this buffer
		 */
		@Override
		public int limit() {
			return buf.buf().limit();
		}

		/**
		 * 
		 * @param newLimit the new limit for this buffer
		 * @return the buffer
		 */
		@Override
		public ByteBuffer limit(int newLimit) {
			autoExpand(newLimit, 0);
			buf.buf().limit(newLimit);
			return this;
		}

		/**
		 * 
		 * @return the mark
		 */
		@Override
		public ByteBuffer mark() {
			buf.buf().mark();
			return this;
		}

		/**
		 * 
		 * @return reset the buffer
		 */
		@Override
		public ByteBuffer reset() {
			buf.buf().reset();
			return this;
		}

		/**
		 * 
		 * @return the buffer
		 */
		@Override
		public ByteBuffer clear() {
			buf.buf().clear();
			return this;
		}

		/**
		 * 
		 * @return the buffer
		 */
		@Override
		public ByteBuffer flip() {
			buf.buf().flip();
			return this;
		}

		/**
		 * 
		 * @return the buffer
		 */
		@Override
		public ByteBuffer rewind() {
			buf.buf().rewind();
			return this;
		}

		/**
		 * 
		 * @return bytes remaining
		 */
		@Override
		public int remaining() {
			return buf.buf().remaining();
		}

		/**
		 * 
		 * @return a new buffer duplicating the original buffer
		 */
		@Override
		public ByteBuffer duplicate() {
			PooledByteBuffer newBuf = allocateContainer();
			newBuf.init(new UnexpandableByteBuffer(buf.buf().duplicate(), buf),
					false);
			return newBuf;
		}

		/**
		 * 
		 * @return a copy of this buffer
		 */
		@Override
		public ByteBuffer slice() {
			PooledByteBuffer newBuf = allocateContainer();
			newBuf.init(new UnexpandableByteBuffer(buf.buf().slice(), buf),
					false);
			return newBuf;
		}

		/**
		 * 
		 * @return buffer as read only buffer
		 */
		@Override
		public ByteBuffer asReadOnlyBuffer() {
			PooledByteBuffer newBuf = allocateContainer();
			newBuf.init(new UnexpandableByteBuffer(
					buf.buf().asReadOnlyBuffer(), buf), false);
			return newBuf;
		}

		/**
		 * 
		 * @return byte at current position
		 */
		@Override
		public byte get() {
			return buf.buf().get();
		}

		/**
		 * 
		 * @param b byte to put
		 * @return original buffer
		 */
		@Override
		public ByteBuffer put(byte b) {
			autoExpand(1);
			buf.buf().put(b);
			return this;
		}

		/**
		 * 
		 * @param index index for byte
		 * @return the byte at position 'index'
		 */
		@Override
		public byte get(int index) {
			return buf.buf().get(index);
		}

		/**
		 * 
		 * @param index index to put byte at
		 * @param b byt to but
		 * @return the buffer
		 */
		@Override
		public ByteBuffer put(int index, byte b) {
			autoExpand(index, 1);
			buf.buf().put(index, b);
			return this;
		}

		/**
		 * 
		 * @param dst where to put gotten data
		 * @param offset offset in this to start getting
		 * @param length number of bytes to get
		 * @return the original byte buffer
		 */
		@Override
		public ByteBuffer get(byte[] dst, int offset, int length) {
			buf.buf().get(dst, offset, length);
			return this;
		}

		/**
		 * 
		 * @param src buffer to copy from
		 * @return the original buffer
		 */
		@Override
		public ByteBuffer put(java.nio.ByteBuffer src) {
			autoExpand(src.remaining());
			buf.buf().put(src);
			return this;
		}

		/**
		 * 
		 * @param src src array to copy from
		 * @param offset offset in source array to copy from
		 * @param length number of bytes to copy
		 * @return original buffer
		 */
		@Override
		public ByteBuffer put(byte[] src, int offset, int length) {
			autoExpand(length);
			buf.buf().put(src, offset, length);
			return this;
		}

		/**
		 * 
		 * @return the original buffer
		 */
		@Override
		public ByteBuffer compact() {
			buf.buf().compact();
			return this;
		}

		/**
		 * 
		 * @param that buffer to compare to
		 * @return <0 if this<that; >0 if this>that; 0 if this==that
		 */
		public int compareTo(ByteBuffer that) {
			return this.buf.buf().compareTo(that.buf());
		}

		/**
		 * 
		 * @return order of bytes in buffer
		 */
		@Override
		public ByteOrder order() {
			return buf.buf().order();
		}

		/**
		 * 
		 * @param bo byte order you want to set
		 * @return the original buffer
		 */
		@Override
		public ByteBuffer order(ByteOrder bo) {
			buf.buf().order(bo);
			return this;
		}

		/**
		 * 
		 * @return char at current position
		 */
		@Override
		public char getChar() {
			return buf.buf().getChar();
		}

		/**
		 * 
		 * @param value char to put
		 * @return original buffer
		 */
		@Override
		public ByteBuffer putChar(char value) {
			autoExpand(2);
			buf.buf().putChar(value);
			return this;
		}

		/**
		 * 
		 * @param index index of char to get from
		 * @return char to get back
		 */
		@Override
		public char getChar(int index) {
			return buf.buf().getChar(index);
		}

		/**
		 * 
		 * @param index index to put char at
		 * @param value value to put
		 * @return origianl buffer
		 */
		@Override
		public ByteBuffer putChar(int index, char value) {
			autoExpand(index, 2);
			buf.buf().putChar(index, value);
			return this;
		}

		/**
		 * 
		 * @return buffer as char buffer
		 */
		@Override
		public CharBuffer asCharBuffer() {
			return buf.buf().asCharBuffer();
		}

		/**
		 * 
		 * @return short at current position
		 */
		@Override
		public short getShort() {
			return buf.buf().getShort();
		}

		/**
		 * 
		 * @param value short to put
		 * @return original buffer
		 */
		@Override
		public ByteBuffer putShort(short value) {
			autoExpand(2);
			buf.buf().putShort(value);
			return this;
		}

		/**
		 * 
		 * @param index index to get short at
		 * @return short at position 'index'
		 */
		@Override
		public short getShort(int index) {
			return buf.buf().getShort(index);
		}

		/**
		 * 
		 * @param index index to put short at
		 * @param value value to put
		 * @return original buffer
		 */
		@Override
		public ByteBuffer putShort(int index, short value) {
			autoExpand(index, 2);
			buf.buf().putShort(index, value);
			return this;
		}

		/**
		 * 
		 * @return buffer as short buffer
		 */
		@Override
		public ShortBuffer asShortBuffer() {
			return buf.buf().asShortBuffer();
		}

		/**
		 * 
		 * @return int at current position
		 */
		@Override
		public int getInt() {
			return buf.buf().getInt();
		}

		/**
		 * 
		 * @param value value to put at current position
		 * @return original buffer
		 */
		@Override
		public ByteBuffer putInt(int value) {
			autoExpand(4);
			buf.buf().putInt(value);
			return this;
		}

		/**
		 * 
		 * @param index index to get int at
		 * @return int at currrent position
		 */
		@Override
		public int getInt(int index) {
			return buf.buf().getInt(index);
		}

		/**
		 * 
		 * @param index position to put at
		 * @param value value to put
		 * @return original buffer
		 */
		@Override
		public ByteBuffer putInt(int index, int value) {
			autoExpand(index, 4);
			buf.buf().putInt(index, value);
			return this;
		}

		/**
		 * 
		 * @return as int buffer
		 */
		@Override
		public IntBuffer asIntBuffer() {
			return buf.buf().asIntBuffer();
		}

		/**
		 * 
		 * @return long at current position
		 */
		@Override
		public long getLong() {
			return buf.buf().getLong();
		}

		/**
		 * 
		 * @param value value to put at current position
		 * @return original buffer
		 */
		@Override
		public ByteBuffer putLong(long value) {
			autoExpand(8);
			buf.buf().putLong(value);
			return this;
		}

		/**
		 * 
		 * @param index index to get at
		 * @return long at position 'index'
		 */
		@Override
		public long getLong(int index) {
			return buf.buf().getLong(index);
		}

		/**
		 * 
		 * @param index index to put value at
		 * @param value value to put
		 * @return original buffer
		 */
		@Override
		public ByteBuffer putLong(int index, long value) {
			autoExpand(index, 8);
			buf.buf().putLong(index, value);
			return this;
		}

		/**
		 * 
		 * @return as long buffer
		 */
		@Override
		public LongBuffer asLongBuffer() {
			return buf.buf().asLongBuffer();
		}

		/**
		 * 
		 * @return float at current position
		 */
		@Override
		public float getFloat() {
			return buf.buf().getFloat();
		}

		/**
		 * 
		 * @param value value to put
		 * @return original buffer with value inserted
		 */
		@Override
		public ByteBuffer putFloat(float value) {
			autoExpand(4);
			buf.buf().putFloat(value);
			return this;
		}

		/**
		 * 
		 * @param index index to get at
		 * @return float at position 'index'
		 */
		@Override
		public float getFloat(int index) {
			return buf.buf().getFloat(index);
		}

		/**
		 * 
		 * @param index index to put at
		 * @param value value to put at
		 * @return byte buffer with value inserted
		 */
		@Override
		public ByteBuffer putFloat(int index, float value) {
			autoExpand(index, 4);
			buf.buf().putFloat(index, value);
			return this;
		}

		/**
		 * 
		 * @return as float buffer
		 */
		@Override
		public FloatBuffer asFloatBuffer() {
			return buf.buf().asFloatBuffer();
		}

		/**
		 * 
		 * @return double at current position
		 */
		@Override
		public double getDouble() {
			return buf.buf().getDouble();
		}

		/**
		 * 
		 * @param value value to put
		 * @return buffer with value inserted
		 */
		@Override
		public ByteBuffer putDouble(double value) {
			autoExpand(8);
			buf.buf().putDouble(value);
			return this;
		}

		/**
		 * 
		 * @param index position to get double at
		 * @return double at position 'index'
		 */
		@Override
		public double getDouble(int index) {
			return buf.buf().getDouble(index);
		}

		/**
		 * 
		 * @param index position to put value at
		 * @param value value to put
		 * @return byte buffer with value inserted
		 */
		@Override
		public ByteBuffer putDouble(int index, double value) {
			autoExpand(index, 8);
			buf.buf().putDouble(index, value);
			return this;
		}

		/**
		 * 
		 * @return as double buffer
		 */
		@Override
		public DoubleBuffer asDoubleBuffer() {
			return buf.buf().asDoubleBuffer();
		}

		/**
		 * 
		 * @param expectedRemaining number of bytes you expect to be remaining
		 * @return the original buffer, expanded to fix expectedRemaining if it doesn't
		 *   fit already
		 */
		@Override
		public ByteBuffer expand(int expectedRemaining) {
			if (autoExpand) {
				int pos = buf.buf().position();
				int limit = buf.buf().limit();
				int end = pos + expectedRemaining;
				if (end > limit) {
					ensureCapacity(end);
					buf.buf().limit(end);
				}
			}
			return this;
		}

		/**
		 * 
		 * @param pos position you'd like to start from
		 * @param expectedRemaining number of bytes you expect to remain after pos
		 * @return this byte buffer expanded to ensure expectedRemaining can be retrieved
		 */
		@Override
		public ByteBuffer expand(int pos, int expectedRemaining) {
			if (autoExpand) {
				int limit = buf.buf().limit();
				int end = pos + expectedRemaining;
				if (end > limit) {
					ensureCapacity(end);
					buf.buf().limit(end);
				}
			}
			return this;
		}

		/**
		 * 
		 * @param requestedCapacity
		 */
		private void ensureCapacity(int requestedCapacity) {
			if (requestedCapacity <= buf.buf().capacity()) {
				return;
			}

			if (buf.isDerived()) {
				throw new IllegalStateException(
						"Derived buffers cannot be expanded.");
			}

			int newCapacity = MINIMUM_CAPACITY;
			while (newCapacity < requestedCapacity) {
				newCapacity <<= 1;
			}

			UnexpandableByteBuffer oldBuf = this.buf;
			UnexpandableByteBuffer newBuf = allocate0(newCapacity, isDirect());
			newBuf.buf().clear();
			newBuf.buf().order(oldBuf.buf().order());

			int pos = oldBuf.buf().position();
			int limit = oldBuf.buf().limit();
			oldBuf.buf().clear();
			newBuf.buf().put(oldBuf.buf());
			newBuf.buf().position(0);
			newBuf.buf().limit(limit);
			newBuf.buf().position(pos);
			this.buf = newBuf;
			oldBuf.release();
		}

		/**
		 * 
		 * @return byte buffer as array if possible, or null if not.
		 */
		@Override
		public byte[] array() {
			// TODO Auto-generated method stub
			return null;
		}

		/**
		 * 
		 * @return offset
		 */
		@Override
		public int arrayOffset() {
			// TODO Auto-generated method stub
			return 0;
		}

		/**
		 * 
		 * @param newCapacity new capacity
		 * @return this with new capacity set.
		 */
		@Override
		public ByteBuffer capacity(int newCapacity) {
			// TODO Auto-generated method stub
			return null;
		}

		/**
		 * 
		 * @return mark value
		 */
		@Override
		public int markValue() {
			// TODO Auto-generated method stub
			return 0;
		}

	}

	/**
     *
     */
	private class UnexpandableByteBuffer {
		/**
         *
         */
		private final java.nio.ByteBuffer buf;

		/**
         *
         */
		private final UnexpandableByteBuffer parentBuf;

		/**
         *
         */
		private int refCount;

		/**
         *
         */
		private boolean pooled;

		/**
		 * 
		 * @param buf
		 */
		protected UnexpandableByteBuffer(java.nio.ByteBuffer buf) {
			this.buf = buf;
			this.parentBuf = null;
		}

		/**
		 * 
		 * @param buf
		 * @param parentBuf
		 */
		protected UnexpandableByteBuffer(java.nio.ByteBuffer buf,
				UnexpandableByteBuffer parentBuf) {
			parentBuf.acquire();
			this.buf = buf;
			this.parentBuf = parentBuf;
		}

		/**
         *
         */
		public void init() {
			refCount = 1;
			pooled = true;
		}

		/**
         *
         */
		public synchronized void acquire() {
			if (isDerived()) {
				parentBuf.acquire();
				return;
			}

			if (refCount <= 0) {
				throw new IllegalStateException("Already released buffer.");
			}

			refCount++;
		}

		/**
         *
         */
		public void release() {
			if (isDerived()) {
				parentBuf.release();
				return;
			}

			synchronized (this) {
				if (refCount <= 0) {
					refCount = 0;
					throw new IllegalStateException(
							"Already released buffer.  You released the buffer too many times.");
				}

				refCount--;
				if (refCount > 0) {
					return;
				}
			}

			// No need to return buffers to the pool if it is disposed already.
			if (disposed) {
				return;
			}

			if (pooled) {
				if (parentBuf != null) {
					release0(parentBuf);
				} else {
					release0(this);
				}
			}
		}

		/**
		 * 
		 * @return as java.nio.ByteBuffer
		 */
		public java.nio.ByteBuffer buf() {
			return buf;
		}

		/**
		 * 
		 * @return is pooled
		 */
		public boolean isPooled() {
			return pooled;
		}

		/**
		 * 
		 * @param pooled set whether pooled
		 */
		public void setPooled(boolean pooled) {
			this.pooled = pooled;
		}

		/**
		 * 
		 * @return is derived
		 */
		public boolean isDerived() {
			return parentBuf != null;
		}
	}
}
