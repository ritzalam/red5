package org.red5.server.api.test;

import org.red5.server.BaseConnection;

public class TestConnection extends BaseConnection {

	public TestConnection(String host, String path, String sessionId){
		super(PERSISTENT,host,null,path,sessionId,null);
	}
	
	public long getReadBytes() {
		return 0;
	}

	public long getWrittenBytes() {
		return 0;
	}

}
