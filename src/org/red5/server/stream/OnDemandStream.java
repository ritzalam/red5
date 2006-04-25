package org.red5.server.stream;

import org.red5.server.api.IScope;
import org.red5.server.api.stream.IOnDemandStream;
import org.red5.server.net.rtmp.RTMPConnection;

public class OnDemandStream extends Stream implements IOnDemandStream {

	public OnDemandStream(IScope scope, IStreamSource source, RTMPConnection conn) {
		super(scope, conn);
		setSource(source);
	}
	
	public void play() {
		play(-1);
	}

	public void play(int length) {
		start(0, length);
	}

	public void resume() {
		resume(getCurrentPosition());
	}

	public boolean isStopped() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isPlaying() {
		return !isPaused() && !isStopped();
	}

}
