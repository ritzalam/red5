package org.red5.server.rtmp;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 *
 * Copyright (c) 2006-2009 by respective authors. All rights reserved.
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

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.object.Deserializer;
import org.red5.io.object.Serializer;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.codec.RTMPProtocolDecoder;
import org.red5.server.net.rtmp.codec.RTMPProtocolEncoder;
import org.red5.server.net.rtmp.event.Invoke;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RTMPTestCase extends TestCase implements Constants {

	protected static Logger log = LoggerFactory.getLogger(RTMPTestCase.class);

	protected RTMPProtocolDecoder decoder;

	protected Deserializer deserializer;

	protected RTMPProtocolEncoder encoder;

	protected Serializer serializer;

	/** {@inheritDoc} */
    @Override
	protected void setUp() throws Exception {
		// TODO Auto-generated method stub
		super.setUp();
		serializer = new Serializer();
		deserializer = new Deserializer();
		encoder = new RTMPProtocolEncoder();
		decoder = new RTMPProtocolDecoder();
		encoder.setSerializer(serializer);
		decoder.setDeserializer(deserializer);
	}

	public void testHeaders() {
		RTMP rtmp = new RTMP(RTMP.MODE_SERVER);
		Header header = new Header();
		header.setChannelId((byte) 0x12);
		header.setDataType(TYPE_INVOKE);
		header.setStreamId(100);
		header.setTimer(2);
		header.setSize(320);
		IoBuffer buf = encoder.encodeHeader(rtmp, header, null);
		buf.flip();
		log.debug(buf.getHexDump());
		Assert.assertNotNull(buf);
		Header result = decoder.decodeHeader(buf, null);
		Assert.assertEquals(header, result);
	}

	public void testInvokePacket() {
		@SuppressWarnings("unused")
		Invoke invoke = new Invoke();
	}

}
