package org.red5.server.stream;

/**
 * A service used to create and manage token buckets.
 * @author The Red5 Project (red5@osflash.org)
 * @author Steven Gong (steven.gong@gmail.com)
 */
public interface ITokenBucketService {
	public static final String KEY = "TokenBucketService";
	
	/**
	 * Create a token bucket.
	 * @param capacity Capacity of the bucket.
	 * @param speed Speed of the bucket. Bytes per millisecond.
	 * @return <tt>null</tt> if fail to create.
	 */
	ITokenBucket createTokenBucket(long capacity, long speed);
	
	/**
	 * Remove this bucket.
	 * @param bucket
	 */
	void removeTokenBucket(ITokenBucket bucket);
}
