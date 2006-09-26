package org.red5.server.rtmp.test;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright © 2006 by respective authors. All rights reserved.
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
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.io.utils.HexDump;
import org.red5.server.net.rtmp.RTMPUtils;

public class RTMPUtilsTest extends TestCase {

	protected static Log log = LogFactory.getLog(RTMPUtilsTest.class.getName());

	public void testDecodingHeader() {

		log.debug("Testing");
		/*
		 log.debug(""+(0x03 >> 6));
		 log.debug(""+(0x43 >> 6));
		 log.debug(""+(0x83 >> 6));
		 log.debug(""+((byte)(((byte)0xC3) >> 6)));
		 */
		byte test;
		test = (byte) (0x03);
		log.debug(HexDump.byteArrayToHexString(new byte[] { test }));
		log.debug("" + test);
		log.debug("" + RTMPUtils.decodeHeaderSize(test));

		test = (byte) (0x43);
		log.debug(HexDump.byteArrayToHexString(new byte[] { test }));
		log.debug("" + test);
		log.debug("" + RTMPUtils.decodeHeaderSize(test));

		test = (byte) (0x83);
		log.debug(HexDump.byteArrayToHexString(new byte[] { test }));
		log.debug("" + test);
		log.debug("" + RTMPUtils.decodeHeaderSize(test));

		test = (byte) (0xC3 - 256);
		log.debug(HexDump.byteArrayToHexString(new byte[] { test }));
		log.debug("" + test);
		log.debug("" + RTMPUtils.decodeHeaderSize(test));

		Assert.assertEquals(true, false);
	}

}
