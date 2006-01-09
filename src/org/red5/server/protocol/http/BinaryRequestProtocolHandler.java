package org.red5.server.protocol.http;

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
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.SessionConfig;
import org.apache.mina.io.IoHandlerAdapter;
import org.apache.mina.io.IoSession;
import org.apache.mina.io.socket.SocketSessionConfig;
import org.red5.server.utils.HexDump;

public abstract class BinaryRequestProtocolHandler extends IoHandlerAdapter {

	protected static Log log =
        LogFactory.getLog(BinaryRequestProtocolHandler.class.getName());
	
	protected static final  Charset CHARSET = Charset.forName("UTF-8");
	
	public void sessionCreated(IoSession session) {
		SessionConfig cfg = session.getConfig();
		if (cfg instanceof SocketSessionConfig) {
			((SocketSessionConfig) cfg).setSessionReceiveBufferSize(2048);
		}
	}

	public void exceptionCaught(IoSession session, Throwable cause) {
		session.close();
	}
	
	public String readString(int from, int to, ByteBuffer buf){
		final java.nio.ByteBuffer strBuf = buf.buf();
		final int pos = buf.position();
		final int limit = buf.limit();
		log.debug("from: "+from+" to:"+to);
		buf.position(from);
		strBuf.limit(to);
		final String string =  CHARSET.decode(strBuf).toString();
		buf.position(pos);
		buf.limit(limit);
		return string;
	}
	
	
	public void dataRead(IoSession ioSession, ByteBuffer in) {
		
		log.debug("New http request");
		
	
		byte CR = 0x0D;
		byte LF = 0x0A;
		
		byte last = 0x00;
		byte current = 0x00;
		
		int mark = 0;
		String line;
		TreeMap headers = new TreeMap();
		boolean firstLine = true;
		String request = "";
		while(in.position()<in.limit()){
			
			current = in.get();
			
			if(current == LF && last == CR){
				if(mark == in.position()-2) break; // end of headers
				line = readString(mark, in.position(), in);
				mark = in.position();
				if(firstLine){
					firstLine = false;
					request = line.substring(0,line.length()-2);
				} else {
					String[] tokens = line.split(": ");
					headers.put(tokens[0], tokens[1].substring(0,tokens[1].length()-2));
				}
			}
			
			last = current;
		}
		
		log.debug(request);
		Iterator keys = headers.keySet().iterator();
		while(keys.hasNext()){
			String key = (String) keys.next();
			String value = (String) headers.get(key);
			log.debug(key+": "+value);
		}
		
		ByteBuffer respHeaders = ByteBuffer.allocate(1024);
		respHeaders.setAutoExpand(true);
		logBuffer("amf",in);
		ByteBuffer respBody = handleRequest(in, request, headers);
		
		Iterator it = headers.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry header = (Map.Entry) it.next();
			//respHeaders.putString((CharSequence) header.getKey(), CHARSET.newEncoder());
			
		}
		
		logBuffer("amf",in);
		
		ioSession.write(respHeaders, null);
		ioSession.write(respBody, null);
		
		log.debug("Response sent");		
	}

	public abstract ByteBuffer handleRequest(ByteBuffer in, String request, Map headers);

	public void logBuffer(String msg, ByteBuffer buf) {
		if(log.isDebugEnabled()){
			log.debug(msg);
			log.debug("Size: " + buf.remaining());
			log.debug(HexDump.formatHexDump(buf.getHexDump()));
		}
	}

}
