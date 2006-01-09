package org.red5.server.utils;

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

import java.nio.charset.Charset;

import org.apache.commons.logging.Log;
import org.apache.mina.common.ByteBuffer;
import org.red5.server.io.amf.AMF;

public class BufferLogUtils {

	public static final Charset CHARSET = Charset.forName("UTF-8");
	
	public static void debug(Log log, String msg, ByteBuffer buf){
		if(log.isDebugEnabled()){
			
			log.debug(msg);
			log.debug("Size: " + buf.remaining());
			log.debug("Data:\n\n" + HexDump.formatHexDump(buf.getHexDump()));
			
			final String string = toString(buf);
			
			log.debug("\n"+string+"\n");
			
			
			//log.debug("Data:\n\n" + b);
		}
	}

	public static String toString(ByteBuffer buf) {
		int pos = buf.position();
		int limit = buf.limit();
		final java.nio.ByteBuffer strBuf = buf.buf();
		final String string = AMF.CHARSET.decode(strBuf).toString();
		buf.position(pos);
		buf.limit(limit);
		return string;
	}
	
}
