package org.red5.server.net.rtmp;

import org.red5.server.net.rtmp.event.IRTMPEvent;

public interface IClientListener {

	public void onClientListenerEvent(IRTMPEvent rtmpEvent);

	public void stopListening();
	
}
