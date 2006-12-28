package org.red5.server.rtmp.test;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.red5.io.object.Deserializer;
import org.red5.io.object.Serializer;
import org.red5.server.net.rtmp.codec.RTMPProtocolDecoder;
import org.red5.server.net.rtmp.codec.RTMPProtocolEncoder;
import org.red5.server.net.rtmp.event.Invoke;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.Header;

public class RTMPTestCase extends TestCase implements Constants {

	protected static Log log = LogFactory.getLog(RTMPTestCase.class.getName());

	protected Serializer serializer;

	protected Deserializer deserializer;

	protected RTMPProtocolEncoder encoder;

	protected RTMPProtocolDecoder decoder;

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
		Header header = new Header();
		header.setChannelId((byte) 0x12);
		header.setDataType(TYPE_INVOKE);
		header.setStreamId(100);
		header.setTimer(2);
		header.setSize(320);
		ByteBuffer buf = encoder.encodeHeader(header, null);
		buf.flip();
		log.debug(buf.getHexDump());
		Assert.assertNotNull(buf);
		Header result = decoder.decodeHeader(buf, null);
		Assert.assertEquals(header, result);
	}

	public void testInvokePacket() {
		Invoke invoke = new Invoke();
	}

}
