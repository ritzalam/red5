package org.electroteque;

import org.electroteque.BandwidthDetection;

import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.IConnection;
import org.red5.server.api.IScope;

import org.electroteque.security.PlaybackSecurity;
import org.electroteque.security.PublishSecurity;
import org.electroteque.security.SharedObjectSecurity;

public class BWApplication extends MultiThreadedApplicationAdapter
{
	@Override
	public boolean appStart(IScope app) {
		registerStreamPublishSecurity(new PublishSecurity());
		registerSharedObjectSecurity(new SharedObjectSecurity());
		registerStreamPlaybackSecurity(new PlaybackSecurity());
		
		return super.appStart(app);
	}
	
	@Override
	public boolean appConnect(IConnection conn, Object[] params) {

		BandwidthDetection detect = new BandwidthDetection();
		detect.checkBandwidth(conn);
		return super.appConnect(conn, params);
	}
}
