package org.red5.server.rtmp.test;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.red5.server.io.Deserializer;
import org.red5.server.io.Serializer;
import org.red5.server.rtmp.codec.ProtocolDecoder;
import org.red5.server.rtmp.codec.ProtocolEncoder;
import org.red5.server.rtmp.message.Constants;
import org.red5.server.rtmp.message.Invoke;
import org.red5.server.rtmp.message.PacketHeader;
import org.red5.server.utils.BufferLogUtils;

public class RTMPTestCase extends TestCase implements Constants {

	protected static Log log =
        LogFactory.getLog(RTMPTestCase.class.getName());
	
	protected Serializer serializer;
	protected Deserializer deserializer;
	protected ProtocolEncoder encoder;
	protected ProtocolDecoder decoder;
	
	protected void setUp() throws Exception {
		// TODO Auto-generated method stub
		super.setUp();
		serializer = new Serializer();
		deserializer = new Deserializer();
		encoder = new ProtocolEncoder();
		decoder = new ProtocolDecoder();
		encoder.setSerializer(serializer);
		decoder.setDeserializer(deserializer);
	}
	
	public void testHeaders(){
		PacketHeader header = new PacketHeader();
		header.setChannelId((byte)0x12);
		header.setDataType(TYPE_INVOKE);
		header.setStreamId(100);
		header.setTimer(2);
		header.setSize(320);
		ByteBuffer buf = encoder.encodeHeader(header,null);
		buf.flip();
		BufferLogUtils.debug(log,"header",buf);
		Assert.assertNotNull(buf);
		PacketHeader result = decoder.decodeHeader(buf, null);
		Assert.assertEquals(header, result);
	}
	
	public void testInvokePacket(){
		Invoke invoke = new Invoke();
	}
	

}
