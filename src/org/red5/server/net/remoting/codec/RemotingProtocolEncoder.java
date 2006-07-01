package org.red5.server.net.remoting.codec;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006 by respective authors (see below). All rights reserved.
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

import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.red5.io.amf.Output;
import org.red5.io.object.Serializer;
import org.red5.server.net.protocol.ProtocolState;
import org.red5.server.net.protocol.SimpleProtocolEncoder;
import org.red5.server.net.remoting.message.RemotingCall;
import org.red5.server.net.remoting.message.RemotingPacket;

public class RemotingProtocolEncoder implements SimpleProtocolEncoder {

	protected static Log log =
        LogFactory.getLog(RemotingProtocolEncoder.class.getName());

	protected static Log ioLog =
        LogFactory.getLog(RemotingProtocolEncoder.class.getName()+".out");
	
	private Serializer serializer = null;
	
	public ByteBuffer encode(ProtocolState state, Object message) throws Exception {
		

		RemotingPacket resp = (RemotingPacket) message;
		Iterator it = resp.getCalls().iterator();
		ByteBuffer buf = ByteBuffer.allocate(1024);
		buf.setAutoExpand(true);
		Output output = new Output(buf);
		buf.putShort((short) 0); // write the version
		buf.putShort((short) 0); // write the header count
		buf.putShort((short) resp.getCalls().size()); // write the number of bodies
		while(it.hasNext()){
			log.debug("Call");
			RemotingCall call = (RemotingCall) it.next();
			Output.putString(buf,call.getClientResponse());
		   	Output.putString(buf,"null");
		   	buf.putInt(-1);
		   	log.info("result:" + call.getResult());
		   	serializer.serialize(output, call.getClientResult());
		}
		//buf.compact();
		buf.flip();
		log.info(">>"+buf.getHexDump());
		return buf;

	}				

	public void dispose(IoSession ioSession) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void setSerializer(Serializer serializer) {
		this.serializer = serializer;
	}	
	
}
