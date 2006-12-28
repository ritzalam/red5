package org.red5.server.net.proxy;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006 by respective authors (see below). All rights reserved.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoSession;

/**
 * Proxy filter
 */
public class ProxyFilter extends IoFilterAdapter {
    /**
     * Forwarding key constant
     */
	public static final String FORWARD_KEY = "proxy_forward_key";
    /**
     * Logger
     */
	protected static Log log = LogFactory.getLog(ProxyFilter.class.getName());
    /**
     * Filter name
     */
	protected String name;

    /**
     * Create proxy filter with given name
     * @param name
     */
	public ProxyFilter(String name) {
		this.name = name;
	}

	/** {@inheritDoc} */
    @Override
	public void messageReceived(NextFilter next, IoSession session,
			Object message) throws Exception {
        // Create forwarding IO session
        IoSession forward = (IoSession) session.getAttribute(FORWARD_KEY);
		if (forward != null && forward.isConnected()) {

			if (message instanceof ByteBuffer) {
				final ByteBuffer buf = (ByteBuffer) message;
				//buf.acquire();

				if (log.isDebugEnabled()) {
					log.debug("[ " + name + " ] RAW >> " + buf.getHexDump());
				}

				ByteBuffer copy = ByteBuffer.allocate(buf.limit());
				int limit = buf.limit();
				copy.put(buf);
				copy.flip();
				forward.write(copy);
				buf.flip();
				buf.position(0);
				buf.limit(limit);
				//buf.flip();
			}

			if (message instanceof ByteBuffer) {
			}
		}
		next.messageReceived(session, message);
	}

	/** {@inheritDoc} */
    @Override
	public void sessionClosed(NextFilter next, IoSession session)
			throws Exception {
		IoSession forward = (IoSession) session.getAttribute(FORWARD_KEY);
		if (forward != null && forward.isConnected() && !forward.isClosing()) {
			if (log.isDebugEnabled()) {
				log.debug("[ " + name + " ] Closing: " + forward);
			}
			forward.close();
		}
		next.sessionClosed(session);
	}

}