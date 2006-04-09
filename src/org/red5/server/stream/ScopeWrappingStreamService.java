package org.red5.server.stream;

import org.red5.server.api.IConnection;
import org.red5.server.api.IScope;
import org.red5.server.api.Red5;
import org.red5.server.api.stream.IBroadcastStream;
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
		
		ISubscriberStream stream = ((IStreamCapableConnection) conn).newSubscriberStream(name, 0);
		stream.start();
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
