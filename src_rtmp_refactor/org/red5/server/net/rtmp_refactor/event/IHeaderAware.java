package org.red5.server.net.rtmp_refactor.event;

import org.red5.server.net.rtmp_refactor.message.Header;

public interface IHeaderAware {

	public void setHeader(Header header);
	
}
