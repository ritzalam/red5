package org.red5.server.api;

public interface OnDemandStream extends Stream {

	public void play();
	public void seek(int position);
	public void pause();
	public void resume(); 
	public void stop();
	
}