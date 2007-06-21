package org.red5.server.net.remoting.codec;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2007 by respective authors (see below). All rights reserved.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.red5.compatibility.flex.messaging.messages.AbstractMessage;
import org.red5.compatibility.flex.messaging.messages.ErrorMessage;
import org.red5.io.amf.Output;
import org.red5.io.object.Serializer;
import org.red5.server.net.protocol.ProtocolState;
import org.red5.server.net.protocol.SimpleProtocolEncoder;
import org.red5.server.net.remoting.FlexMessagingService;
import org.red5.server.net.remoting.message.RemotingCall;
import org.red5.server.net.remoting.message.RemotingPacket;
import org.red5.server.service.ServiceNotFoundException;

/**
 * Remoting protocol encoder
 */
public class RemotingProtocolEncoder implements SimpleProtocolEncoder {
    /**
     * Logger
     */
	protected static Log log = LogFactory.getLog(RemotingProtocolEncoder.class.getName());
    /**
     * I/O logger
     */
	protected static Log ioLog = LogFactory.getLog(RemotingProtocolEncoder.class.getName() + ".out");

    /**
     * Data serializer
     */
    private Serializer serializer;

	/** {@inheritDoc} */
    public ByteBuffer encode(ProtocolState state, Object message) throws Exception {
		RemotingPacket resp = (RemotingPacket) message;
		ByteBuffer buf = ByteBuffer.allocate(1024);
		buf.setAutoExpand(true);
		Output output;
		buf.putShort((short) 0); // write the version
		buf.putShort((short) 0); // write the header count
		buf.putShort((short) resp.getCalls().size()); // write the number of bodies
		for (RemotingCall call: resp.getCalls()) {
			if (log.isDebugEnabled()) {
				log.debug("Call");
			}
			Output.putString(buf, call.getClientResponse());
			Output.putString(buf, "null");
			buf.putInt(-1);
			if (log.isDebugEnabled()) {
				log.info("result:" + call.getResult());
			}
			if (call.isAMF3) {
				output = new org.red5.io.amf3.Output(buf);
			} else {
				output = new Output(buf);
			}
			Object result = call.getClientResult();
			if (call.isMessaging && !call.isSuccess() && !(result instanceof ErrorMessage)) {
				// Generate proper error result for the Flex messaging client
				AbstractMessage request = (AbstractMessage) call.getArguments()[0];
				if (result instanceof ServiceNotFoundException) {
					ServiceNotFoundException ex = (ServiceNotFoundException) result;
					if (FlexMessagingService.SERVICE_NAME.equals(ex.getServiceName())) {
						result = FlexMessagingService.returnError(request, "serviceNotAvailable", "Flex messaging not activated", ex.getMessage());
					} else {
						// This should never happen as the service name is hardcoded...
						result = FlexMessagingService.returnError(request, "serviceNotAvailable", "Flex messaging not activated", ex.getMessage());
					}
				} else {
					result = FlexMessagingService.returnError(request, "error", result.toString(), result.toString());
				}
			}
			serializer.serialize(output, result);
		}
		//buf.compact();
		buf.flip();
		if (log.isDebugEnabled()) {
			log.debug(">>" + buf.getHexDump());
		}
		return buf;

	}

    /**
     * Dispose I/O session, not implemented yet
     * @param ioSession         I/O session
     * @throws Exception        Exception
     */
	public void dispose(IoSession ioSession) throws Exception {
		// TODO Auto-generated method stub
	}

	/**
     * Setter for serializer.
     *
     * @param serializer  New serializer
     */
    public void setSerializer(Serializer serializer) {
		this.serializer = serializer;
	}

}
