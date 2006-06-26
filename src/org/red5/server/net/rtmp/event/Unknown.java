package org.red5.server.net.rtmp.event;

import org.apache.mina.common.ByteBuffer;
import org.red5.io.utils.HexDump;

public class Unknown extends BaseEvent {
	
	protected ByteBuffer data = null;
	protected byte dataType;
	
	public Unknown(byte dataType, ByteBuffer data) {
		super(Type.SYSTEM);
		this.dataType = dataType;
		this.data = data;
	}
	
	public byte getDataType() {
		return dataType;
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

	public void release() {
		if (data != null)
			data.release();
		super.release();
	}
}