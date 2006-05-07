package org.red5.server.stream;

import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.stream.ISubscriberStreamNew;
import org.red5.server.api.stream.IStream;
import org.red5.server.api.stream.IStreamCapableConnection;
import org.red5.server.api.stream.IStreamService;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.RTMPHandler;

public class StreamServiceNew implements IStreamService {

	public int createStream() {
		IConnection conn = Red5.getConnectionLocal();
		if (!(conn instanceof IStreamCapableConnection))
			return -1;
		
		return ((IStreamCapableConnection) conn).reserveStreamId();
	}

	public void closeStream() {
		IConnection conn = Red5.getConnectionLocal();
		if (!(conn instanceof IStreamCapableConnection))
			return;
		IStream stream = ((IStreamCapableConnection) conn).getStreamById(getCurrentStreamId());
		stream.close();
	}

	public void deleteStream(int number) {
		IConnection conn = Red5.getConnectionLocal();
		if (!(conn instanceof IStreamCapableConnection))
			return;
		
		deleteStream((IStreamCapableConnection) conn, number);
	}

	public void deleteStream(IStreamCapableConnection conn, int number) {
		IStream stream = conn.getStreamById(number);
		if (stream == null) {
			// XXX: invalid request, we must return an error to the client here...
		}
		conn.deleteStreamById(number);
		stream.close();
	}

	public void play(String name) {
		play(name, new Double(-2000.0), -1, false);

	}

	public void play(String name, Double type) {
		play(name, type, -1, false);

	}

	public void play(String name, Double type, int length) {
		play(name, type, length, false);
	}

	public void play(String name, Double type, int length, boolean flushPlaylist) {
		IConnection conn = Red5.getConnectionLocal();
		if (!(conn instanceof IStreamCapableConnection))
			return;
		
		// it seems as if the number is sent multiplied by 1000
		int num = (int)(type.doubleValue() / 1000.0);
		if (num < -2)
			num = -2;
		RTMPConnection rtmpConn = (RTMPConnection) conn;
		
		int streamId = getCurrentStreamId();
		IStream stream = rtmpConn.getStreamById(streamId);
		if (stream != null) {
			if (!(stream instanceof ISubscriberStreamNew)) {
				// XXX override a nonstandard stream or ignore
				// ignore for now
				return;
			}
			if (flushPlaylist) {
				stream.close();
			}
		} else {
			stream = rtmpConn.newSubscriberStreamNew(streamId);
		}
		SubscriberStreamNew ss = (SubscriberStreamNew) stream;
		ss.addPlayItem(name, num, length);
		if ((ss.getStatus() & ISubscriberStreamNew.INIT) != 0) {
			ss.start();
		}
	}

	public void pause(boolean pausePlayback, int position) {
		IConnection conn = Red5.getConnectionLocal();
		if (!(conn instanceof IStreamCapableConnection))
			return;
		IStream stream = ((IStreamCapableConnection) conn).getStreamById(getCurrentStreamId());
		if (stream instanceof ISubscriberStreamNew) {
			if (pausePlayback) ((ISubscriberStreamNew) stream).pause(position);
			else ((ISubscriberStreamNew) stream).resume(position);
		}
	}

	public void seek(int position) {
		IConnection conn = Red5.getConnectionLocal();
		if (!(conn instanceof IStreamCapableConnection))
			return;
		IStream stream = ((IStreamCapableConnection) conn).getStreamById(getCurrentStreamId());
		if (stream instanceof ISubscriberStreamNew) {
			((ISubscriberStreamNew) stream).seek(position);
		}
	}

	public void publish(String name) {
		// TODO Auto-generated method stub

	}

	public void publish(String name, String mode) {
		// TODO Auto-generated method stub

	}

	public void publish(boolean dontStop) {
		// TODO Auto-generated method stub

	}

	private int getCurrentStreamId() {
		// TODO: this must come from the current connection!
		return RTMPHandler.getStreamId();
	}
}
