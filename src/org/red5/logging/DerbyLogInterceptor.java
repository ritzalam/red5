package org.red5.logging;

/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright (c) 2006-2010 by respective authors (see below). All rights reserved.
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

import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DerbyLogInterceptor {

	protected static Logger log = LoggerFactory.getLogger(DerbyLogInterceptor.class);
	
	private static ThreadLocal<StringBuilder> local = new ThreadLocal<StringBuilder>();
	
	public static OutputStream handleDerbyLogFile(){
	    return new OutputStream() {
	    	
	        @Override
			public void write(byte[] b) throws IOException {
	        	log.info("Derby log: {}", new String(b));
			}

	        @Override
			public void write(int i) throws IOException {
				StringBuilder sb = local.get();
				if (sb == null) {
					sb = new StringBuilder();
				}
				//look for LF
				if (i == 10) {
					log.info("Derby log: {}", sb.toString());
					sb.delete(0, sb.length() - 1);
				} else {
					log.trace("Derby log: {}", i); 
					sb.append(new String(intToDWord(i)));
				}				
				local.set(sb);
	        }
	    };
	}
	
	private static byte[] intToDWord(int i) {
		byte[] dword = new byte[4];
		dword[0] = (byte) (i & 0x00FF);
		dword[1] = (byte) ((i >> 8) & 0x000000FF);
		dword[2] = (byte) ((i >> 16) & 0x000000FF);
		dword[3] = (byte) ((i >> 24) & 0x000000FF);
		return dword;
	}
	
}
