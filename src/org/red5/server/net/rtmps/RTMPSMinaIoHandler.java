package org.red5.server.net.rtmps;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2009 by respective authors (see below). All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.NotActiveException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.ssl.SslFilter;
import org.red5.server.net.protocol.ProtocolState;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.RTMPHandshake;
import org.red5.server.net.rtmp.RTMPMinaConnection;
import org.red5.server.net.rtmp.RTMPMinaIoHandler;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.message.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles Native RTMPS protocol events fired by the MINA framework.
 * <pre>
 * var nc:NetConnection = new NetConnection();
 * nc.proxyType = "best";
 * nc.connect("rtmps:\\localhost\app");
 * </pre>
 * Originally created by: Kevin Green
 *  
 * @author Kevin Green (kevygreen@gmail.com)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class RTMPSMinaIoHandler extends RTMPMinaIoHandler {

	private static Logger log = LoggerFactory.getLogger(RTMPSMinaIoHandler.class);

	/**
	 * Password for accessing the keystore.
	 */
	private char[] password;
	
	/**
	 * Stores the keystore file bytes.
	 */
	private byte[] keystore;
	
	/** {@inheritDoc} */
	@Override
	public void sessionOpened(IoSession session) throws Exception {
		super.sessionOpened(session);

		if (password == null || keystore == null) {
			throw new NotActiveException("Keystore or password are null");
		}
		
		// START OF NATIVE SSL STUFF
		SSLContext context = SSLContext.getInstance("TLSv1");
		// The reference implementation only supports X.509 keys
		KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
		// Sun's default kind of key store
		KeyStore ks = KeyStore.getInstance("JKS");
		// For security, every key store is encrypted with a
		// pass phrase that must be provided before we can load
		// it from disk. The pass phrase is stored as a char[] array
		// so it can be wiped from memory quickly rather than
		// waiting for a garbage collector. Of course using a string
		// literal here completely defeats that purpose.
		ks.load(new ByteArrayInputStream(keystore), password);
		kmf.init(ks, password);
		// initialize the ssl context
		context.init(kmf.getKeyManagers(), null, null);
		// 
		SslFilter sslFilter = new SslFilter(context);
		session.getFilterChain().addFirst("sslFilter", sslFilter);
		// END OF NATIVE SSL STUFF

		RTMP rtmp = (RTMP) session.getAttribute(ProtocolState.SESSION_KEY);
		if (rtmp.getMode() == RTMP.MODE_CLIENT) {
			log.debug("Handshake 1st phase");
			IoBuffer out = IoBuffer.allocate(Constants.HANDSHAKE_SIZE + 1);
			out.put((byte) 0x03);
			out.put(RTMPHandshake.getHandshakeBytes());
			out.flip();
			session.write(out);
		} else {
			final RTMPMinaConnection conn = (RTMPMinaConnection) session
					.getAttribute(RTMPConnection.RTMP_CONNECTION_KEY);
			handler.connectionOpened(conn, rtmp);
		}
	}

	/**
	 * Password used to access the keystore file.
	 * 
	 * @param password
	 */
	public void setKeyStorePassword(String password) {
		this.password = password.toCharArray();
	}
	
	/**
	 * Set keystore data from a file.
	 * 
	 * @param file contains keystore
	 */
	public void setKeystoreFile(String path) {
		FileInputStream fis = null;
		try {			
			File file = new File(path);
			if (file.exists()) {
    			fis = new FileInputStream(file);
    			FileChannel fc = fis.getChannel();
    			ByteBuffer fb = ByteBuffer.allocate(Long.valueOf(file.length()).intValue());
    			fc.read(fb);
    			fb.flip();
    			keystore = IoBuffer.wrap(fb).array();
			} else {
				log.warn("Keystore file does not exist: {}", path);
			}
			file = null;
		} catch (Exception e) {
			log.warn("Error setting keystore data", e);
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
				}
			}
		}
	}

	/**
	 * Set keystore data from a file.
	 * 
	 * @param arr keystore bytes
	 */
	public void setKeystoreBytes(byte[] arr) {
		keystore = new byte[arr.length];
		System.arraycopy(arr, 0, keystore, 0, arr.length);
	}
	
}
