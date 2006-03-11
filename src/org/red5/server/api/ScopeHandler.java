package org.red5.server.api;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright © 2006 by respective authors (see below). All rights reserved.
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

/**
 * The Scope Handler Controls actions performed against a scope object, and also
 * is notified of all events Gives fine grained control over what actions can be
 * performed with the can* methods Allows for detailed reporting on what is
 * happening within the scope with the on* methods This is the core interface
 * users implement to create applications The thread local connection is always
 * available via the Red5 object within these methods
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard (luke@codegent.com)
 */
public interface ScopeHandler {

	/**
	 * Can a new scope be created for a given context path
	 * 
	 * @param contextPath
	 *            the context path, eg: /myapp/room
	 * @return true if the scope can be created, otherwise false
	 */
	boolean canCreateScope(String contextPath);

	/**
	 * Called when a scope is created for the first time
	 * 
	 * @param scope
	 *            the new scope object
	 */
	void onCreateScope(Scope scope);

	/**
	 * Called just before a scope is disposed
	 */
	void onDisposeScope(Scope scope);

	/**
	 * Can a given client connect to a scope
	 * 
	 * @param conn
	 *            the connection object
	 * @return true if the client can connect, otherwise false
	 */
	boolean canConnect(Connection conn, Scope scope);

	/**
	 * Called just after a client has connected to a scope
	 * 
	 * @param conn
	 *            connection object
	 */
	void onConnect(Connection conn);

	/**
	 * Called just before the client disconnects from the scope
	 * 
	 * @param conn
	 *            connection object
	 */
	void onDisconnect(Connection conn);

	/**
	 * Can the service call proceed
	 * 
	 * @param call
	 *            call object holding service name, method, and arguments
	 * @return true if the client can call the service, otherwise false
	 */
	boolean canCallService(Call call);

	/**
	 * Called just before a service call This is a chance to modify the call
	 * object
	 * 
	 * @param call
	 *            the call object
	 * @return same or modified call object
	 */
	Call preProcessServiceCall(Call call);

	/**
	 * Called when a service is called
	 * 
	 * @param call
	 *            the call object
	 */
	void onServiceCall(Call call);

	/**
	 * Called just after a service call This is a chance to modify the result
	 * object
	 * 
	 * @param call
	 * @return same or modified call object
	 */
	Call postProcessServiceCall(Call call);

	/**
	 * Can an event be broadcast to all connected clients
	 * 
	 * @param event
	 *            the event object
	 * @return true if the broadcast can continue
	 */
	boolean canBroadcastEvent(Object event);

	/**
	 * Called when an event is broadcast
	 * 
	 * @param event
	 *            the event object
	 */
	void onEventBroadcast(Object event);

	// The following methods will only be called for RTMP connections

	/**
	 * Can the stream be published
	 * 
	 * @param name
	 *            of the stream
	 * @return true if the client can publish the stream, otherwise false
	 */
	boolean canPublishStream(String name);

	/**
	 * Called when the client begins publishing
	 * 
	 * @param stream
	 *            the stream object
	 */
	void onStreamPublishStart(Stream stream);

	/**
	 * Called when the client stops publishing
	 * 
	 * @param stream
	 *            the stream object
	 */
	void onStreamPublishStop(Stream stream);

	/**
	 * Can the client broadcast a stream
	 * 
	 * @param name
	 *            name of the stream
	 * @return true if the broadcast is allowed, otherwise false
	 */
	boolean canBroadcastStream(String name);

	/**
	 * Called when the broadcast starts
	 * 
	 * @param stream
	 *            the stream object
	 */
	void onBroadcastStreamStart(Stream stream);

	/**
	 * Can a client record a stream with a given name
	 * 
	 * @param name
	 *            name of the stream to be recorded, usually the name of the FLV
	 * @return true if the record can continue, otherwise false
	 */
	boolean canRecordStream(String name);

	/**
	 * Called when a recording starts
	 * 
	 * @param stream
	 *            the stream object
	 */
	void onRecordStreamStart(Stream stream);

	/**
	 * Called when a recording stops
	 * 
	 * @param stream
	 *            the stream object
	 */
	void onRecordStreamStop(Stream stream);

	/**
	 * Can a client subscribe to a broadcast stream
	 * 
	 * @param name
	 *            the name of the stream
	 * @return true if they can subscribe, otherwise false
	 */
	boolean canSubscribeToBroadcastStream(String name);

	/**
	 * Called when a client subscribes to a broadcast
	 * 
	 * @param stream
	 *            the stream object
	 */
	void onBroadcastStreamSubscribe(BroadcastStream stream);

	/**
	 * Called when a client unsubscribes from a broadcast
	 * 
	 * @param stream
	 *            the stream object
	 */
	void onBroadcastStreamUnsubscribe(BroadcastStream stream);

	/**
	 * Can a client connect to an on demand stream
	 * 
	 * @param name
	 *            the name of the stream, this is normally the path to the FLV
	 * @return true if the connect can continue, otherwise false to reject
	 */
	boolean canConnectToOnDemandStream(String name);

	/**
	 * Called when a client connects to an on demand stream
	 * 
	 * @param stream
	 *            the stream object
	 */
	void onOnDemandStreamConnect(OnDemandStream stream);

	/**
	 * Called when a client disconnects from an on demand stream
	 * 
	 * @param stream
	 *            the stream object
	 */
	void onOnDemandStreamDisconnect(OnDemandStream stream);

	/**
	 * Can a client connect to a shared object
	 * 
	 * @param soName
	 *            the name of the shared object, since it may not exist yet
	 * @return true if they can connect, otherwise false
	 */
	boolean canConnectSharedObject(String soName);

	/**
	 * Called when a client connects to a shared object
	 * 
	 * @param so
	 *            the shared object
	 */
	void onSharedObjectConnect(SharedObject so);

	/**
	 * Can a shared object attribute be updated
	 * 
	 * @param so
	 *            the shared object be updated
	 * @param key
	 *            the name of the attribute
	 * @param value
	 *            the value of the attribute
	 * @return true if the update can continue
	 */
	boolean canUpdateSharedObject(SharedObject so, String key, Object value);

	/**
	 * Called when a shared object attribute is updated
	 * 
	 * @param so
	 *            the shared object
	 * @param key
	 *            the name of the attribute
	 * @param value
	 *            the value of the attribute
	 */
	void onSharedObjectUpdate(SharedObject so, String key, Object value);

	/**
	 * Can the client delete a shared object attribute
	 * 
	 * @param so
	 *            the shared object
	 * @param key
	 *            the name of the attribute to be deleted
	 * @return true if the delete can continue, otherwise false
	 */
	boolean canDeleteSharedObject(SharedObject so, String key);

	/**
	 * Called when an attribute is deleted from the shared object
	 * 
	 * @param so
	 *            the shared object
	 * @param key
	 *            the name of the attribute to delete
	 */
	void onSharedObjectDelete(SharedObject so, String key);

	/**
	 * Can a shared object send continue
	 * 
	 * @param so
	 *            the shared object
	 * @param method
	 *            the method name
	 * @param params
	 *            the arguments
	 * @return true if the send can continue, otherwise false
	 */
	boolean canSendSharedObject(SharedObject so, String method, Object[] params);

	/**
	 * Called when a shared object method call is sent
	 * 
	 * @param so
	 *            the shared object
	 * @param method
	 *            the method name to call
	 * @param params
	 *            the arguments
	 */
	void onSharedObjectSend(SharedObject so, String method, Object[] params);

}