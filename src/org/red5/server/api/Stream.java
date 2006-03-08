package org.red5.server.api;

public interface Stream {
	
	public int getCurrentPosition();
	public boolean hasAudio();
	public boolean hasVideo();
	public String getVideoCodec();
	public String getAudioCodec();
	public Scope getScope();
	public void close();

}
