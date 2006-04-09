package org.red5.server.stream;

import org.red5.server.api.IConnection;
import org.red5.server.api.IScope;
import org.red5.server.api.Red5;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IOnDemandStream;
import org.red5.server.api.stream.IStream;
import org.red5.server.api.stream.IStreamCapableConnection;
import org.red5.server.api.stream.IStreamService;
import org.red5.server.api.stream.ISubscriberStream;

public class ScopeWrappingStreamService extends ScopeWrappingStreamManager
		implements IStreamService {

	public ScopeWrappingStreamService(IScope scope) {
		super(scope);
	}
	
	public int createStream() {
		IConnection conn = Red5.getConnectionLocal();
		if (!(conn instanceof IStreamCapableConnection))
			return -1;
		
		// Flash starts with 1
		return ((IStreamCapableConnection) conn).reserveStreamId() + 1;
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
		log.debug("Delete stream: "+stream+" number: "+number);
		deleteStream(stream);
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

	public void play(String name, Double type, int length,
			boolean flushPlaylist) {
		IConnection conn = Red5.getConnectionLocal();
		if (!(conn instanceof IStreamCapableConnection))
			return;

		// it seems as if the number is sent multiplied by 1000
		int num = (int)(type.doubleValue() / 1000.0);
		if (num < -2)
			num = -2;

		boolean isPublishedStream = hasBroadcastStream(name);
		boolean isFileStream = hasOnDemandStream(name);
		
		// decision: 0 for Live, 1 for File, 2 for Wait, 3 for N/A
		int decision = 3;
		
		switch (num) {
		case -2:
			if (isPublishedStream) {
				decision = 0;
			} else if (isFileStream) {
				decision = 1;
			} else {
				decision = 2;
			}
			break;
			
		case -1:
			if (isPublishedStream) {
				decision = 0;
			} else {
				// TODO: Wait for stream to be created until timeout, otherwise continue
				// with next item in playlist (see Macromedia documentation)
				// NOTE: For now we create a temporary stream
				decision = 2;
			}
			break;
			
		default:
			if (isFileStream) {
				decision = 1;
			} else {
				// TODO: Wait for it, then continue with next item in playlist (?)
			}
			break;
		}
		
		ISubscriberStream subscriber;
		IOnDemandStream onDemand;
		switch (decision) {
		case 0:
			subscriber = ((IStreamCapableConnection) conn).newSubscriberStream(name, 0);
			subscriber.start(0, length);
			break;
			
		case 1:
			onDemand = ((IStreamCapableConnection) conn).newOnDemandStream(name, 0);
			// TODO: initial seeking?
			onDemand.play(length);
			break;
			
		case 2:
			subscriber = ((IStreamCapableConnection) conn).newSubscriberStream(name, 0);
			subscriber.start(0, length);
			break;
		}
	}

	public void publish(String name, String mode) {
		IConnection conn = Red5.getConnectionLocal();
		if (!(conn instanceof IStreamCapableConnection))
			return;
		
		if (!mode.equals("live"))
			// TODO: impement other stream modes
			return;
		
		// TODO: where can I get the current client id from?
		IBroadcastStream stream = ((IStreamCapableConnection) conn).newBroadcastStream(name, 0);
		
	}

}
