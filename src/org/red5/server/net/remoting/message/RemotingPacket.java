package org.red5.server.net.remoting.message;

import java.nio.ByteBuffer;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

public class RemotingPacket {

	protected HttpServletRequest request;
	protected ByteBuffer data;
	protected List calls;
	protected String scopePath;
	
	public RemotingPacket(List calls) {
		this.calls = calls;
	}

	public List getCalls() {
		return calls;
	}

	public void setScopePath(String path) {
		scopePath = path;
	}
	
	public String getScopePath() {
		return scopePath;
	}
	
}
