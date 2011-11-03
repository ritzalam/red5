package org.red5.io.mp4.impl;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import junit.framework.TestCase;

import org.junit.Test;
import org.red5.io.ITag;
import org.red5.io.flv.IKeyFrameDataAnalyzer.KeyFrameMeta;
import org.red5.io.mp4.impl.MP4Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MP4ReaderTest extends TestCase {

	private static Logger log = LoggerFactory.getLogger(MP4ReaderTest.class);

	@Test
	public void testCtor() throws Exception {
		File file = new File("fixtures/sample.mp4");
		// contains version 1 sound description 
		//File file = new File("E:/dev/red5/java/server/trunk/distx/webapps/oflaDemo/streams/transformers_720p.mp4");
		// contains version 0 sound description 
		//File file = new File("E:/dev/red5/java/server/trunk/distx/webapps/oflaDemo/streams/mp4_with_aac.mp4");
		MP4Reader reader = new MP4Reader(file);

		KeyFrameMeta meta = reader.analyzeKeyFrames();
		log.debug("Meta: {}", meta);
		
		ITag tag = reader.readTag();
		log.debug("Tag: {}", tag);
		tag = reader.readTag();
		log.debug("Tag: {}", tag);
		tag = reader.readTag();
		log.debug("Tag: {}", tag);
		tag = reader.readTag();
		log.debug("Tag: {}", tag);

		log.info("----------------------------------------------------------------------------------");

		//File file2 = new File("E:/media/test_clips/IronMan.mov");
		//MP4Reader reader2 = new MP4Reader(file2, false);

		// log.info("----------------------------------------------------------------------------------");

		//File file3 = new File("fixtures/AdobeBand_300K_H264.mp4");
		//MP4Reader reader3 = new MP4Reader(file3);

		//tag = reader3.readTag();		
		//log.debug("Tag: {}", tag);		
		//tag = reader3.readTag();		
		//log.debug("Tag: {}", tag);

	}

	@Test
	public void testBytes() throws Exception {
		//00 40 94 00 00 00 00 00 00 00 06 == 
		byte width[] = { (byte) 0x00, (byte) 0x40, (byte) 0x94, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };
		System.out.println("width: {}" + bytesToLong(width));

		//		byte height[] = { (byte) 0x40, (byte) 0x86, (byte) 0x80, (byte) 0x00 };
		//		System.out.println("height: {}" + bytesToInt(height));
		//
		//		byte timescale[] = { (byte) 0x40, (byte) 0xA7, (byte) 0x6A, (byte) 0x00 };
		//		System.out.println("timescale: {}" + bytesToInt(timescale));
		//
		//		byte duration[] = { (byte) 0x40, (byte) 0x6D, (byte) 0xE9, (byte) 0x03,
		//				(byte) 0x22, (byte) 0x7B, (byte) 0x4C, (byte) 0x47 };
		//		System.out.println("duration: {}" + bytesToLong(duration));
		//
		//		byte avcprofile[] = { (byte) 0x40, (byte) 0x53, (byte) 0x40,
		//				(byte) 0x00 };
		//		System.out.println("avcprofile: {}" + bytesToInt(avcprofile));
		//
		//		byte avclevel[] = { (byte) 0x40, (byte) 0x49, (byte) 0x80, (byte) 0x00 };
		//		System.out.println("avclevel: {}" + bytesToInt(avclevel));
		//
		//		byte aacaot[] = { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x40 };
		//		System.out.println("aacaot: {}" + bytesToLong(aacaot));
		//
		//		byte videoframerate[] = { (byte) 0x40, (byte) 0x37, (byte) 0xF9,
		//				(byte) 0xDB, (byte) 0x22, (byte) 0xD0, (byte) 0xE5, (byte) 0x60 };
		//		System.out.println("videoframerate: {}" + bytesToLong(videoframerate));
		//
		//		byte audiochannels[] = { (byte) 0x40, (byte) 0x00, (byte) 0x00,
		//				(byte) 0x00 };
		//		System.out.println("audiochannels: {}" + bytesToInt(audiochannels));
		//
		//		byte moovposition[] = { (byte) 0x40, (byte) 0x40, (byte) 0x00,
		//				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };
		//		System.out.println("moovposition: {}" + bytesToLong(moovposition));
		//
		//		
		//byte[] arr = {(byte) 0x0f};
		//System.out.println("bbb: {}" + bytesToByte(arr));
		//byte[] arr = {(byte) 0xE5, (byte) 0x88, (byte) 0x80, (byte) 0x00, 
		//(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };
		//System.out.println("bbb: {}" + bytesToLong(arr));
		byte[] arr = { (byte) 0, (byte) 0, (byte) 0x10, (byte) 0 };
		System.out.println("bbb: {}" + bytesToInt(arr));
	}

	public static long bytesToLong(byte[] data) {
		ByteBuffer buf = ByteBuffer.allocate(8);
		buf.put(data);
		buf.order(ByteOrder.BIG_ENDIAN);
		buf.flip();
		return buf.getLong();
	}

	public static int bytesToInt(byte[] data) {
		ByteBuffer buf = ByteBuffer.allocate(4);
		buf.put(data);
		buf.order(ByteOrder.BIG_ENDIAN);
		buf.flip();
		return buf.getInt();
	}

	public static short bytesToShort(byte[] data) {
		ByteBuffer buf = ByteBuffer.allocate(2);
		buf.put(data);
		buf.flip();
		return buf.getShort();
	}

	public static byte bytesToByte(byte[] data) {
		ByteBuffer buf = ByteBuffer.allocate(1);
		buf.put(data);
		buf.flip();
		return buf.get();
	}
}
