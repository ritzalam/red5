package org.red5.server.net.rtmp_refactor.event;

import org.apache.mina.common.ByteBuffer;
import org.red5.io.utils.HexDump;

public class Unknown {
	
	protected ByteBuffer data = null;
	
	public Unknown(ByteBuffer data){
		this.data = data;
	}
	
	public ByteBuffer getData(){
		return data;
	}
	
	public String toString(){
		final ByteBuffer buf = getData();
		StringBuffer sb = new StringBuffer();
		sb.append("Size: " + buf.remaining());
		sb.append("Data:\n\n" + HexDump.formatHexDump(buf.getHexDump()));
		return sb.toString();
	}
	
}