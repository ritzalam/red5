package org.red5.server.stream;


public class TokenBucket implements ITokenBucket {
	private long speed;
	private long capacity;
	private long tokens = 0;
	private WaitObject waitObject = null;
	
	public TokenBucket(ITokenBucketService service) {
	}
	
	synchronized public boolean acquireToken(long tokenCount, long wait) {
		if (wait > 0) {
			// as of now, we don't support blocking mode
			throw new IllegalArgumentException("blocking wait unsupported");
		}
		if (tokens >= tokenCount) {
			tokens -= tokenCount;
			return true;
		} else {
			return false;
		}
	}

	synchronized public boolean acquireTokenNonblocking(long tokenCount,
			ITokenBucketCallback callback) {
		if (waitObject != null) return false;
		if (tokens >= tokenCount) {
			tokens -= tokenCount;
			return true;
		} else {
			if (callback != null) {
				waitObject = new WaitObject();
				waitObject.callback = callback;
				waitObject.tokenCount = tokenCount;
			}
			return false;
		}
	}

	public long getCapacity() {
		return capacity;
	}

	public long getSpeed() {
		return speed;
	}

	synchronized public void reset() {
		waitObject = null;
		tokens = 0;
	}

	void setCapacity(long capacity) {
		this.capacity = capacity;
	}
	
	void setSpeed(long speed) {
		this.speed = speed;
	}
	
	/**
	 * Add some tokens to this bucket.
	 * @param token
	 */
	synchronized void addToken(long token) {
		if (tokens + token > capacity) {
			tokens = capacity;
		} else {
			tokens += token;
		}
		if (waitObject != null && tokens >= waitObject.tokenCount) {
			ITokenBucketCallback callback = waitObject.callback;
			long tokenCount = waitObject.tokenCount;
			waitObject = null;
			callback.run(this, tokenCount);
		}
	}
	
	private class WaitObject {
		private ITokenBucketCallback callback;
		private long tokenCount;
	}
}
