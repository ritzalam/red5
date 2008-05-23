package org.red5.server.net.rtmps;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2008 by respective authors (see below). All rights reserved.
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

import org.apache.mina.filter.SSLFilter;
import org.red5.server.net.rtmp.RTMPMinaTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transport setup class configures socket acceptor and thread pools for RTMPS.
 * 
 * @author Paul Gregoire (mondain@gmail.com) 
 */
public class RTMPSMinaTransport extends RTMPMinaTransport {

	private static final Logger log = LoggerFactory
			.getLogger(RTMPSMinaTransport.class);
	
	private static String[] cipherSuites;

	@Override
	public void start() throws Exception {
		if (ioHandler == null) {
			log.info("No RTMP IO Handler associated - using defaults");
			ioHandler = new RTMPSMinaIoHandler();
		}
		super.start();
		log.info("Adding SSL filter");
		SSLFilter sslFilter = new SSLFilter(BogusSSLContextFactory
				.getInstance(true));

		//http://java.sun.com/javase/6/docs/technotes/guides/security/jsse/JSSERefGuide.html
		//http://java.sun.com/javase/6/docs/technotes/guides/security/StandardNames.html#Cipher
		//http://java.sun.com/j2se/1.5.0/docs/guide/security/jsse/JSSERefGuide.html#AppA
		log.info("Adding supported cipher suites");
		sslFilter.setEnabledCipherSuites(cipherSuites);
		
		acceptor.getFilterChain().addFirst("sslFilter", sslFilter);
		log.info("SSL ON");
	}
	
	public void setCipherSuites(String ciphers) {
		log.debug("Setting ciphers: {}", ciphers);
		RTMPSMinaTransport.cipherSuites = ciphers.split(",");
		if (log.isDebugEnabled()) {
			for (String s : cipherSuites) {
				log.debug("Cipher suite: {}", s);
			}
		}
	}
	
	public String toString() {
		return "RTMPS Mina Transport [port=" + port + "]";
	}

}