package org.red5.server.net.rtmp.event;

import org.red5.server.api.event.IEvent;
import org.red5.server.net.rtmp.message.Header;

public interface IRTMPEvent extends IEvent {

	public byte getDataType();
	
	public Header getHeader();
	
	public void setHeader(Header header);
	
	public int getTimestamp();
	
	public void setTimestamp(int timestamp);
	
	/**
	 * Hook to free buffers allocated by the event.
	 */
	public void release();
	
}
