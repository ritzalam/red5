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

import junit.framework.TestCase;

import org.red5.io.utils.HexDump;
import org.red5.server.net.rtmp.RTMPUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */
public class RTMPUtilsTest extends TestCase {

	protected static Logger log = LoggerFactory.getLogger(RTMPUtilsTest.class);

	public void testDecodingHeader() {

		if (log.isDebugEnabled()) {
			log.debug("Testing");
			/*
			 log.debug(""+(0x03 >> 6));
			 log.debug(""+(0x43 >> 6));
			 log.debug(""+(0x83 >> 6));
			 log.debug(""+((byte)(((byte)0xC3) >> 6)));
			 */
		}
		byte test;
		test = 0x03;
		if (log.isDebugEnabled()) {
			log.debug(HexDump.byteArrayToHexString(new byte[] { test }));
			log.debug("" + test);
			log.debug("" + RTMPUtils.decodeHeaderSize(test, 1));
		}
		assertEquals(0, RTMPUtils.decodeHeaderSize(test, 1));
		test = (byte) (0x43);
		if (log.isDebugEnabled()) {
			log.debug(HexDump.byteArrayToHexString(new byte[] { test }));
			log.debug("" + test);
			log.debug("" + RTMPUtils.decodeHeaderSize(test, 1));
		}
		assertEquals(1, RTMPUtils.decodeHeaderSize(test, 1));
		test = (byte) (0x83);
		if (log.isDebugEnabled()) {
			log.debug(HexDump.byteArrayToHexString(new byte[] { test }));
			log.debug("" + test);
			log.debug("" + RTMPUtils.decodeHeaderSize(test, 1));
		}
		assertEquals(-2, RTMPUtils.decodeHeaderSize(test, 1));
		test = (byte) (0xC3 - 256);
		if (log.isDebugEnabled()) {
			log.debug(HexDump.byteArrayToHexString(new byte[] { test }));
			log.debug("" + test);
			log.debug("" + RTMPUtils.decodeHeaderSize(test, 1));
		}
		assertEquals(-1, RTMPUtils.decodeHeaderSize(test, 1));
	}

}
