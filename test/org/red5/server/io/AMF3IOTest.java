package org.red5.server.io;

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

import java.util.List;

import junit.framework.Assert;

import org.apache.mina.core.buffer.IoBuffer;
import org.junit.Test;
import org.red5.io.amf3.Input;
import org.red5.io.amf3.Output;
import org.red5.io.utils.HexDump;

/*
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 * @author Art Clarke, Vlideshow Inc (aclarke@vlideshow.com)
*/
public class AMF3IOTest extends AbstractIOTest {

	IoBuffer buf;

	/** {@inheritDoc} */
	@Override
	void dumpOutput() {
		buf.flip();
		System.err.println(HexDump.formatHexDump(buf.getHexDump()));
	}

	/** {@inheritDoc} */
	@Override
	void resetOutput() {
		setupIO();
	}

	/** {@inheritDoc} */
	@Override
	void setupIO() {
		buf = IoBuffer.allocate(0); // 1kb
		buf.setAutoExpand(true);
		buf.setAutoShrink(true);
		in = new Input(buf);
		out = new Output(buf);
	}

	@Test
	public void testVectorIntInput() {
		log.debug("Testing Vector<int>");
		//0D090000000002000007D07FFFFFFF80000000
		byte[] v = new byte[] { (byte) 0x0D, (byte) 0x09, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0xD0, (byte) 0x7F, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

		in = new Input(IoBuffer.wrap(v));

		List<Object> vectorOut = deserializer.deserialize(in, null);
		//[2, 2000, 2147483647, -2147483648]
		Assert.assertNotNull(vectorOut);
		Assert.assertEquals(vectorOut.size(), 4);
		for (int i = 0; i < vectorOut.size(); i++) {
			System.err.println("Vector: " + vectorOut.get(i));
		}
		resetOutput();
	}

	@Test
	public void testVectorUIntInput() {
		log.debug("Testing Vector<uint>");
		//0E090000000002000007D0FFFFFFFF00000000
		byte[] v = new byte[] { (byte) 0x0E, (byte) 0x09, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0xD0, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

		in = new Input(IoBuffer.wrap(v));

		List<Object> vectorOut = deserializer.deserialize(in, null);
		//[2, 2000, 4294967295, 0]
		Assert.assertNotNull(vectorOut);
		Assert.assertEquals(vectorOut.size(), 4);
		for (int i = 0; i < vectorOut.size(); i++) {
			System.err.println("Vector: " + vectorOut.get(i));
		}
		resetOutput();
	}

	@Test
	public void testVectorNumberInput() {
		log.debug("Testing Vector<Number>");
		//0F0F003FF199999999999ABFF199999999999A7FEFFFFFFFFFFFFF0000000000000001FFF8000000000000FFF00000000000007FF0000000000000
		byte[] v = new byte[] { (byte) 0x0F, (byte) 0x0F, (byte) 0x00, (byte) 0x3F, (byte) 0xF1, (byte) 0x99,
				(byte) 0x99, (byte) 0x99, (byte) 0x99, (byte) 0x99, (byte) 0x9A, (byte) 0xBF, (byte) 0xF1, (byte) 0x99,
				(byte) 0x99, (byte) 0x99, (byte) 0x99, (byte) 0x99, (byte) 0x9A, (byte) 0x7F, (byte) 0xEF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0xFF, (byte) 0xF8, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0xF0, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7F, (byte) 0xF0, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

		in = new Input(IoBuffer.wrap(v));

		List<Object> vectorOut = deserializer.deserialize(in, null);
		//[1.1, -1.1, 1.7976931348623157E308, 4.9E-324, NaN, -Infinity, Infinity]
		Assert.assertNotNull(vectorOut);
		Assert.assertEquals(vectorOut.size(), 7);
		for (int i = 0; i < vectorOut.size(); i++) {
			System.err.println("Vector: " + vectorOut.get(i));
		}
		resetOutput();
	}

	@Test
	public void testVectorStringInput() {
		log.debug("Testing Vector<String>");
		//10090001060D666F6F5265660106000609666F6F33
		byte[] v = new byte[] { (byte) 0x10, (byte) 0x09, (byte) 0x00, (byte) 0x01, (byte) 0x06, (byte) 0x0D,
				(byte) 0x66, (byte) 0x6F, (byte) 0x6F, (byte) 0x52, (byte) 0x65, (byte) 0x66, (byte) 0x01, (byte) 0x06,
				(byte) 0x00, (byte) 0x06, (byte) 0x09, (byte) 0x66, (byte) 0x6F, (byte) 0x6F, (byte) 0x33 };

		in = new Input(IoBuffer.wrap(v));

		List<Object> vectorOut = deserializer.deserialize(in, null);
		//[foo, null, fooRef, foo3]
		Assert.assertNotNull(vectorOut);
		Assert.assertEquals(vectorOut.size(), 4);
		for (int i = 0; i < vectorOut.size(); i++) {
			System.err.println("Vector: " + vectorOut.get(i));
		}
		resetOutput();
	}
	
	//Vector.<Vector.<int>>
	//100500010D07000000004E00000000000000150D030000000022
	//[[78, 0, 21], [34]]

	//Vector.<com.test>
	//10070011636F6D2E74657374010A130007666F6F04020A010403
	//[com.test@1027b4d, com.test@1ed2ae8, com.test@19c26f5]

}
