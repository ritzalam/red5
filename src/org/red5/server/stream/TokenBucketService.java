package org.red5.server.stream;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import sun.misc.Timeable;

public class TokenBucketService extends TimerTask
implements ITokenBucketService, ApplicationContextAware {
	private static final long INTERVAL = 10;
	private ApplicationContext appCtx;
	private Timer timer = null;
	private List<TokenBucket> buckets = new ArrayList<TokenBucket>();

	public ITokenBucket createTokenBucket(long capacity, long speed) {
		TokenBucket bucket = new TokenBucket(this);
		bucket.setCapacity(capacity);
		bucket.setSpeed(speed);
		synchronized (buckets) {
			buckets.add(bucket);
		}
		return bucket;
	}
	
	public void removeTokenBucket(ITokenBucket bucket) {
		synchronized (buckets) {
			buckets.remove(bucket);
		}
	}

	public void setApplicationContext(ApplicationContext appCtx)
			throws BeansException {
		this.appCtx = appCtx;
	}
	
	public void init() {
		timer = new Timer("TokenBucketService", true);
		timer.schedule(this, INTERVAL, INTERVAL);
	}

	@Override
	public void run() {
		synchronized (buckets) {
			for (TokenBucket bucket : buckets) {
				try {
					bucket.addToken(bucket.getSpeed() * 10);
				} catch (Throwable t) {
				}
			}
		}
	}

}
