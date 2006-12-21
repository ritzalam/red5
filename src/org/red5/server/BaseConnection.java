package org.red5.server;

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

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.api.IBasicScope;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.IScope;
import org.red5.server.api.event.IEvent;

/**
 * Base abstract class for connections. Adds connection specific functionality like work with clients
 * to AttributeStore.
 */
public abstract class BaseConnection extends AttributeStore implements
		IConnection {
    /**
     *  Logger
     */
	protected static Log log = LogFactory
			.getLog(BaseConnection.class.getName());
    /**
     *  Connection type
     */
	protected String type;
    /**
     *  Connection host
     */
	protected String host;
    /**
     *  Connection remote address
     */
	protected String remoteAddress;
    /**
     *  Remote port
     */
	protected int remotePort;
    /**
     *  Path of scope client connected to
      */
    protected String path;
    /**
     *  Connection session identifier
     */
	protected String sessionId;
    /**
     *  Number of read messages
     */
	protected long readMessages = 0;
    /**
     *  Number of written messages
     */
	protected long writtenMessages = 0;
    /**
     *  Number of dropped messages
     */
	protected long droppedMessages = 0;
    /**
     *  Connection params passed from client with NetConnection.connect call
     *
     * @see  NetConnection in Flash Media Server Server-side ActionScript guide
     */
	@SuppressWarnings({"JavadocReference"})
    protected Map<String, Object> params = null;
    /**
     *  Client bound to connection
     */
	protected IClient client = null;
    /**
     *  Scope that connection belongs to
     */
	protected Scope scope = null;
    /**
     *  Set of basic scopes.
     */
	protected Set<IBasicScope> basicScopes;

    /**
     *
     * @param type                Connection type
     * @param host                Host
     * @param remoteAddress       Remote address
     * @param remotePort          Remote port
     * @param path                Scope path on server
     * @param sessionId           Session id
     * @param params              Params passed from client
     */
	public BaseConnection(String type, String host, String remoteAddress,
			int remotePort, String path, String sessionId,
			Map<String, Object> params) {
		this.type = type;
		this.host = host;
		this.remoteAddress = remoteAddress;
		this.remotePort = remotePort;
		this.path = path;
		this.sessionId = sessionId;
		this.params = params;
		this.basicScopes = new HashSet<IBasicScope>();
	}

    /**
     * Initializes client
     * @param client        Client bound to connection
     */
	public void initialize(IClient client) {
		if (this.client != null && this.client instanceof Client) {
			// Unregister old client
			((Client) this.client).unregister(this);
		}
		this.client = client;
		if (this.client instanceof Client) {
			// Register new client
			((Client) this.client).register(this);
		}
	}

    /**
     *
     * @return
     */
	public String getType() {
		return type;
	}

    /**
     *
     * @return
     */
	public String getHost() {
		return host;
	}

    /**
     *
     * @return
     */
	public String getRemoteAddress() {
		return remoteAddress;
	}

    /**
     *
     * @return
     */
	public int getRemotePort() {
		return remotePort;
	}

    /**
     *
     * @return
     */
	public String getPath() {
		return path;
	}

    /**
     *
     * @return
     */
	public String getSessionId() {
		return sessionId;
	}

    /**
     * Return connection parameters
     * @return
     */
	public Map<String, Object> getConnectParams() {
		return Collections.unmodifiableMap(params);
	}

    /**
     *
     * @return
     */
	public IClient getClient() {
		return client;
	}

    /**
     * Check whether connection is alive
     * @return       true if connection is bound to scope, false otherwise
     */
	public boolean isConnected() {
		return scope != null;
	}

    /**
     * Connect to another scope on server
     * @param newScope     New scope
     * @return             true on success, false otherwise
     */
	public boolean connect(IScope newScope) {
		return connect(newScope, null);
	}

    /**
     * Connect to another scope on server with given parameters
     * @param newScope        New scope
     * @param params          Parameters to connect with
     * @return                true on success, false otherwise
     */
	public boolean connect(IScope newScope, Object[] params) {
		final Scope oldScope = scope;
		scope = (Scope) newScope;
		if (scope.connect(this, params)) {
			if (oldScope != null) {
				oldScope.disconnect(this);
			}
			return true;
		} else {
			scope = oldScope;
			return false;
		}
	}

    /**
     *
     * @return
     */
	public IScope getScope() {
		return scope;
	}

    /**
     *  Closes connection
     */
	public void close() {

		if (scope != null) {

			log.debug("Close, disconnect from scope, and children");
			try {
                // Unregister all child scopes first
                Set<IBasicScope> tmpScopes = new HashSet<IBasicScope>(
						basicScopes);
				for (IBasicScope basicScope : tmpScopes) {
					unregisterBasicScope(basicScope);
				}
			} catch (Exception err) {
				log.error("Error while unregistering basic scopes.", err);
			}

            // Disconnect
            try {
				scope.disconnect(this);
			} catch (Exception err) {
				log.error("Error while disconnecting from scope " + scope, err);
			}

            // Unregister client
            if (client != null && client instanceof Client) {
				((Client) client).unregister(this);
				client = null;
			}

			scope = null;
		} else {
			log.debug("Close, not connected nothing to do.");
		}

	}

    /**
     * Notified on event
     * @param event       Event
     */
	public void notifyEvent(IEvent event) {
		// TODO Auto-generated method stub		
	}

    /**
     * Dispatches event
     * @param event       Event
     */
	public void dispatchEvent(IEvent event) {

	}

    /**
     * Handles event
     * @param event        Event
     * @return             true if associated scope was able to handle event, false otherwise
     */
	public boolean handleEvent(IEvent event) {
		return getScope().handleEvent(event);
	}

    /**
     *
     * @return
     */
	public Iterator<IBasicScope> getBasicScopes() {
		return basicScopes.iterator();
	}

    /**
     * Registers basic scope
     * @param basicScope      Basic scope to register
     */
	public void registerBasicScope(IBasicScope basicScope) {
		basicScopes.add(basicScope);
		basicScope.addEventListener(this);
	}

    /**
     * Unregister basic scope
     *
     * @param basicScope      Unregister basic scope
     */
	public void unregisterBasicScope(IBasicScope basicScope) {
		basicScopes.remove(basicScope);
		basicScope.removeEventListener(this);
	}

    /**
     *
     * @return
     */
	public abstract long getReadBytes();

    /**
     *
     * @return
     */
	public abstract long getWrittenBytes();

    /**
     *
     * @return
     */
	public long getReadMessages() {
		return readMessages;
	}

    /**
     *
     * @return
     */
	public long getWrittenMessages() {
		return writtenMessages;
	}

    /**
     *
     * @return
     */
	public long getDroppedMessages() {
		return droppedMessages;
	}

    /**
     *
     * @return
     */
	public long getPendingMessages() {
		return 0;
	}

    /**
     * 
     * @param streamId
     * @return
     */
	public long getPendingVideoMessages(int streamId) {
		return 0;
	}

}
