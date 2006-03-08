package org.red5.server.net.rtmp.message;

import org.apache.mina.common.ByteBuffer;
import org.red5.io.utils.BufferUtils;
import org.red5.io.utils.HexDump;

public class Unknown extends Message {
	
	public Unknown(byte dataType){
		super(dataType,1024);
	}
	
	protected void doRelease() {
		// TODO Auto-generated method stub
		super.doRelease();
	}
	
	public String toString(){
		final ByteBuffer buf = getData();
		StringBuffer sb = new StringBuffer();
		sb.append("Size: " + buf.remaining());
		sb.append("Data:\n\n" + HexDump.formatHexDump(buf.getHexDump()));
		return sb.toString();
	}
	
}
