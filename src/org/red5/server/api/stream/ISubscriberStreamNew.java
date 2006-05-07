package org.red5.server.api.stream;

import org.red5.server.api.IConnection;

/**
 * Standard stream for play as a list
 * @author steven
 */
public interface ISubscriberStreamNew extends IStream {
	// capability mask
	public static final int NO_CAP = 0;
	public static final int PAUSABLE = 1;
	public static final int SEEKABLE = 2;
	public static final int STOPPABLE = 4;
	// status
	public static final int INVALID = 0;
	public static final int INIT = 1;
	public static final int PAUSED = 2;
	public static final int PLAYING = 3;
	public static final int STOPPED = 4;
	
	int getCapability();
	void seek(int position);
	void pause(int position);
	void resume(int position);
	void stop();
	int getStatus();
	IConnection getConnection();
}
