package org.red5.server.net.mrtmp;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2008 by respective authors (see below). All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either version 2.1 of the License, or (at your option) any later 
 * version. 
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this library; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 */

import org.apache.mina.common.ByteBuffer;
import org.red5.server.net.rtmp.message.Packet;

/**
 * @author Steven Gong (steven.gong@gmail.com)
 */
public class MRTMPPacket {
	public static final short CONNECT = 0;
	public static final short CLOSE = 1;
	public static final short RTMP = 2;
	
	public static final short JAVA_ENCODING = 0;
	
	public static final int COMMON_HEADER_LENGTH = 20;
	public static final int RTMP_HEADER_LENGTH = COMMON_HEADER_LENGTH + 4;
	
	private Header header;
	private Body body;
	
	static public class Header {
		private short type;
		private short bodyEncoding;
		private boolean dynamic;
		private int clientId;
		private int headerLength;
		private int bodyLength;
		
		public int getBodyLength() {
			return bodyLength;
		}
		
		public void setBodyLength(int bodyLength) {
			this.bodyLength = bodyLength;
		}
		
		public int getClientId() {
			return clientId;
		}
		
		public void setClientId(int clientId) {
			this.clientId = clientId;
		}
		
		public int getHeaderLength() {
			return headerLength;
		}
		
		public void setHeaderLength(int headerLength) {
			this.headerLength = headerLength;
		}
		
		public short getType() {
			return type;
		}
		
		public void setType(short type) {
			this.type = type;
		}

		public short getBodyEncoding() {
			return bodyEncoding;
		}

		public void setBodyEncoding(short bodyEncoding) {
			this.bodyEncoding = bodyEncoding;
		}

		public boolean isDynamic() {
			return dynamic;
		}

		public void setDynamic(boolean dynamic) {
			this.dynamic = dynamic;
		}
		
	}
	
	static public class Body {
		private ByteBuffer rawBuf;

		public ByteBuffer getRawBuf() {
			return rawBuf;
		}

		public void setRawBuf(ByteBuffer rawBuf) {
			this.rawBuf = rawBuf;
		}
		
	}
	
	static public class RTMPHeader extends Header {
		private int rtmpType;

		public int getRtmpType() {
			return rtmpType;
		}

		public void setRtmpType(int rtmpType) {
			this.rtmpType = rtmpType;
		}
		
	}
	
	static public class RTMPBody extends Body {
		private Packet rtmpPacket;

		public Packet getRtmpPacket() {
			return rtmpPacket;
		}

		public void setRtmpPacket(Packet rtmpPacket) {
			this.rtmpPacket = rtmpPacket;
		}
	}

	public Body getBody() {
		return body;
	}

	public void setBody(Body body) {
		this.body = body;
	}

	public Header getHeader() {
		return header;
	}

	public void setHeader(Header header) {
		this.header = header;
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("MRTMPPacket: type=");
		switch (header.getType()) {
			case CONNECT:
				buf.append("CONNECT");
				break;
			case CLOSE:
				buf.append("CLOSE");
				break;
			case RTMP:
				buf.append("RTMP");
				break;
			default:
				break;
		}
		buf.append(",isDynamic=" + header.isDynamic());
		buf.append(",clientId=" + header.getClientId());
		if (header.getType() == RTMP) {
			RTMPHeader rtmpHeader = (RTMPHeader) header;
			buf.append(",rtmpType=" + rtmpHeader.rtmpType);
			RTMPBody rtmpBody = (RTMPBody) body;
			buf.append(",rtmpBody=" + rtmpBody.rtmpPacket.getMessage());
		}

		return buf.toString();
	}
}
