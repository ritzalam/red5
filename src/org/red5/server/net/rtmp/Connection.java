package org.red5.server.net.rtmp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.red5.server.net.rtmp.message.OutPacket;

public class Connection extends BaseConnection {

	protected static Log log =
        LogFactory.getLog(Connection.class.getName());

	private IoSession ioSession;
	
	public Connection(IoSession protocolSession){
		this.ioSession = protocolSession;
	}
	
	public IoSession getIoSession() {
		return ioSession;
	}

	public void write(OutPacket packet){
		ioSession.write(packet);
	}
	
	public void write(ByteBuffer packet){
		ioSession.write(packet);
	}
}
