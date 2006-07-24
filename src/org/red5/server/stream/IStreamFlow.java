package org.red5.server.stream;

import org.red5.server.stream.message.RTMPMessage;

public interface IStreamFlow {

	public int getMaxTimeBuffer();

	public void setMaxTimeBuffer(int maxTimeBuffer);

	public int getMinTimeBuffer();

	public void setMinTimeBuffer(int minTimeBuffer);

	public int getDataBitRate();

	public int getSegmentBytesTransfered();

	public int getSegmentDataTime();

	public long getSegmentStreamTime();

	public int getStreamBitRate();

	public boolean isBufferTimeIncreasing();

	public long getTotalBytesTransfered();

	public long getTotalDataTime();

	public long getTotalStreamTime();
	
	public int getBufferTime();

	public void reset();
	
	public void clear();
	
	public void update(RTMPMessage msg);
	
}