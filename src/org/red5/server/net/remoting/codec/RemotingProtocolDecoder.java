package org.red5.server.net.remoting.codec;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 *
 * Copyright (c) 2006-2009 by respective authors (see below). All rights reserved.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.amf.AMF;
import org.red5.io.object.Deserializer;
import org.red5.io.object.Input;
import org.red5.server.net.protocol.ProtocolState;
import org.red5.server.net.remoting.FlexMessagingService;
import org.red5.server.net.remoting.message.RemotingCall;
import org.red5.server.net.remoting.message.RemotingPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemotingProtocolDecoder {
    /**
     * Logger
     */
	protected static Logger log = LoggerFactory.getLogger(RemotingProtocolDecoder.class);

	/**
     * Data deserializer
     */
	private Deserializer deserializer;

	/**
     * Setter for deserializer.
     *
     * @param deserializer  Deserializer
     */
    public void setDeserializer(Deserializer deserializer) {
		this.deserializer = deserializer;
	}

	/**
	 * Decodes the given buffer.
	 * 
	 * @param state
	 * @param buffer
	 * @return a List of {@link RemotingPacket} objects.
	 */
    public List<Object> decodeBuffer(ProtocolState state, IoBuffer buffer) {
		List<Object> list = new LinkedList<Object>();
		Object packet = null;
		try {
			packet = decode(state, buffer);
		} catch (Exception e) {
			log.error("Decoding error", e);
			packet = null;
		}
		if (packet != null) {
			list.add(packet);
		}
		return list;
	}

	/**
	 * Decodes the buffer and returns a remoting packet.
	 * 
	 * @param state
	 * @param in
	 * @return A {@link RemotingPacket}
	 * @throws Exception
	 */
    public Object decode(ProtocolState state, IoBuffer in) throws Exception {
		Map<String, Object> headers = readHeaders(in);
		List<RemotingCall> calls = decodeCalls(in);
		return new RemotingPacket(headers, calls);
	}

    /**
     * Read remoting headers.
     * 
     * @param in         Input data as byte buffer
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> readHeaders(IoBuffer in) {
		int version = in.getUnsignedShort(); // skip the version
		int count = in.getUnsignedShort();
		log.debug("Read headers - version: {} count: {}", version, count);
		if (count == 0) {
			// No headers present
			return Collections.EMPTY_MAP;
		}
		
		Deserializer deserializer = new Deserializer();
		Input input;
		if (version == 3) {
			input = new org.red5.io.amf3.Input(in);
		} else {
			input = new org.red5.io.amf.Input(in);
		}
		Map<String, Object> result = new HashMap<String, Object>();
		for (int i = 0; i < count; i++) {
			String name = org.red5.io.amf.Input.getString(in);
			boolean required = in.get() == 0x01;
			int size = in.getInt();
			Object value = deserializer.deserialize(input, Object.class);
			if (log.isDebugEnabled()) {
				log.debug("Header: {} Required: {} Size: {} Value: {}", new Object[]{name, required, size, value});
			}
			result.put(name, value);
		}
		return result;
	}

    /**
     * Decode calls.
	 *
     * @param in         Input data as byte buffer
     * @return           List of pending calls
     */
	protected List<RemotingCall> decodeCalls(IoBuffer in) {
		log.debug("Decode calls");
		//in.getInt();
		List<RemotingCall> calls = new LinkedList<RemotingCall>();
		org.red5.io.amf.Input input;
		int count = in.getUnsignedShort();
		log.debug("Calls: {}", count);
		int limit = in.limit();

		// Loop over all the body elements
		for (int i = 0; i < count; i++) {

			in.limit(limit);

			String serviceString = org.red5.io.amf.Input.getString(in);
			String clientCallback = org.red5.io.amf.Input.getString(in);
			log.debug("callback: {}", clientCallback);

            Object[] args = null;
			boolean isAMF3 = false;
			
			@SuppressWarnings("unused") 
			int length = in.getInt();
			// Set the limit and deserialize
			// NOTE: disabled because the FP sends wrong values here
			/*
			 * if (length != -1) in.limit(in.position()+length);
			 */
			byte type = in.get();
			if (type == AMF.TYPE_ARRAY) {
    			int elements = in.getInt();
    			List<Object> values = new ArrayList<Object>();
    			for (int j=0; j<elements; j++) {
    				byte amf3Check = in.get();
    				in.position(in.position()-1);
    				isAMF3 = (amf3Check == AMF.TYPE_AMF3_OBJECT);
    				if (isAMF3) {
    					input = new org.red5.io.amf3.Input(in);
    				} else {
    					input = new org.red5.io.amf.Input(in);
    				}
    				// Prepare remoting mode
    				input.reset();
    				
    				values.add(deserializer.deserialize(input, Object.class));
    			}

    			args = values.toArray(new Object[values.size()]);
    			if (log.isDebugEnabled()) {
    				for (Object element : args) {
    					log.debug("> " + element);
    				}
    			}

            } else if (type == AMF.TYPE_NULL) {
                log.debug("Got null amf type");
                            
            } else if (type != AMF.TYPE_ARRAY) {
				throw new RuntimeException("AMF0 array type expected but found " + type);
			}
		
			String serviceName;
			String serviceMethod;
			int dotPos = serviceString.lastIndexOf('.');
			if (dotPos != -1) {
				serviceName = serviceString.substring(0, dotPos);
				serviceMethod = serviceString.substring(dotPos + 1,
						serviceString.length());
			} else {
				serviceName = "";
				serviceMethod = serviceString;
			}

			boolean isMessaging = false;
			if ("".equals(serviceName) && "null".equals(serviceMethod)) {
				// Use fixed service and method name for Flex messaging requests,
				// this probably will change in the future.
				serviceName = FlexMessagingService.SERVICE_NAME;
				serviceMethod = "handleRequest";
				isMessaging = true;
			}
			log.debug("Service: {} Method: {}", serviceName, serviceMethod);

			// Add the call to the list
			calls.add(new RemotingCall(serviceName, serviceMethod, args, clientCallback, isAMF3, isMessaging));
		}
		return calls;
	}

}
