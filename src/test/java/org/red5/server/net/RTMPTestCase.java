/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2012 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.server.net;

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
		super.setUp();
		serializer = new Serializer();
		deserializer = new Deserializer();
		encoder = new RTMPProtocolEncoder();
		decoder = new RTMPProtocolDecoder();
		encoder.setSerializer(serializer);
		decoder.setDeserializer(deserializer);
	}

	public void testHeaders() {
		RTMP rtmp = new RTMP();
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
