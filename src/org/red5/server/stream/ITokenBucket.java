package org.red5.server.stream;

/**
 * A token bucket that is used to control bandwidth.
 * @author The Red5 Project (red5@osflash.org)
 * @author Steven Gong (steven.gong@gmail.com)
 */
public interface ITokenBucket {
	/**
	 * Acquire tokens amount of <tt>tokenCount</tt>
	 * waiting <tt>wait</tt> milliseconds if token not available.
	 * @param tokenCount The count of tokens to acquire.
	 * @param wait Milliseconds to wait. <tt>0</tt> means no wait
	 * and any value below zero means wait forever. 
	 * @return <tt>true</tt> if successfully acquired or <tt>false</tt>
	 * if not acquired.
	 */
	boolean acquireToken(long tokenCount, long wait);
	
	/**
	 * Nonblockingly acquire token. If the token is not available and
	 * <tt>task</tt> is not null, the callback will be executed when the token
	 * is available.
	 * @param tokenCount
	 * @param callback
	 * @return <tt>true</tt> if successfully acquired or <tt>false</tt>
	 * if not acquired.
	 */
	boolean acquireTokenNonblocking(long tokenCount, ITokenBucketCallback callback);
	
	/**
	 * Get the capacity of this bucket in Byte.
	 * @return
	 */
	long getCapacity();
	
	/**
	 * The amount of tokens increased per second in millisecond.
	 * @return
	 */
	long getSpeed();
	
	/**
	 * Reset this token bucket. All pending threads are woken up with <tt>false</tt>
	 * returned for acquiring token and callback is removed w/o calling back.
	 */
	void reset();
	
	public interface ITokenBucketCallback {
		void run(ITokenBucket bucket, long tokenCount);
	}
}
