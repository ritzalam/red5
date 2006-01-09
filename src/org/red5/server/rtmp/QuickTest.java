package org.red5.server.rtmp;

import org.apache.mina.common.ByteBuffer;
import org.red5.server.io.amf.Input;

public class QuickTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//[01 8D 14 5B] [01 C2 14 A1] [01 C5 79 D9] [01 D2 7A 81] [01 D4 12 2F] [01 D4 C5 5D] [01 D6 7C 15]
	    //[01 D7 A6 34] [01 D7 A8 B8] [01 D7 AB 54] [01 D7 AE 6E]
		//[00 .00 00 0B 00 00 00 00 08. 00] [00 00 00 0B 00 00 00 00 08 00]
		byte[] bytes = new byte[]{
				(byte)0x00,(byte)0x00,(byte)0x0D,(byte)0x00,
				(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x08		
		};
		ByteBuffer data = ByteBuffer.allocate(8);
		data.put(bytes);
		data.flip();
		Input input = new Input(data);
		
		Double dbl = (Double) input.readNumber();
		System.err.println(Double.doubleToLongBits(dbl.doubleValue()));
		
		ByteBuffer data1 = ByteBuffer.allocate(4);
		data1.put((byte)0x01).put((byte)0x8D).put((byte)0x14).put((byte)0x5B);
		data1.flip();
		System.err.println(data1.getInt());
	}

}
