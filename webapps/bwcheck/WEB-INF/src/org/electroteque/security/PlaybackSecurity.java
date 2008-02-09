package org.electroteque.security;

import org.red5.server.api.IScope;
import org.red5.server.api.stream.IStreamPlaybackSecurity;

public class PlaybackSecurity implements IStreamPlaybackSecurity {
	
	public boolean isPlaybackAllowed(IScope scope, String name, int start, int length, boolean flushPlaylist) {
		return false;
	}
  
}

