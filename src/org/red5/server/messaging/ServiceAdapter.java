package org.red5.server.messaging;

/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright (c) 2006-2011 by respective authors (see below). All rights reserved.
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

import org.red5.compatibility.flex.messaging.messages.CommandMessage;
import org.red5.compatibility.flex.messaging.messages.Message;

/**
 * The ServiceAdapter class is the base definition of a service adapter.
 * 
 * @author Paul Gregoire
 */
public abstract class ServiceAdapter {
	
	/**
	 * Starts the adapter if its associated Destination is started and if the adapter 
	 * is not already running. If subclasses override, they must call super.start().
	 */
	public void start() {
		
	}
	
	/**
	 * Stops the ServiceAdapter. If subclasses override, they must call super.start().
	 */
	public void stop() {
		
	}
	
	/**
	 * Handle a data message intended for this adapter. This method is responsible for
	 * handling the message and returning a result (if any). The return value of this 
	 * message is used as the body of the acknowledge message returned to the client. It 
	 * may be null if there is no data being returned for this message.
	 * Typically the data content for the message is stored in the body property of the 
	 * message. The headers of the message are used to store fields which relate to the 
	 * transport of the message. The type of operation is stored as the operation property 
	 * of the message.
	 * 
	 * @param message the message as sent by the client intended for this adapter
	 * @return the body of the acknowledge message (or null if there is no body)
	 */
	public abstract Object invoke(Message message);
	
	/**
	 * Accept a command from the adapter's service and perform some internal 
	 * action based upon it. CommandMessages are used for messages which control 
	 * the state of the connection between the client and the server. For example,
	 * this handles subscribe, unsubscribe, and ping operations. The messageRefType
	 * property of the CommandMessage is used to associate a command message with a
	 * particular service. Services are configured to handle messages of a particular 
	 * concrete type. For example, the MessageService is typically invoked to handle
	 * messages of type flex.messaging.messages.AsyncMessage. To ensure a given 
	 * CommandMessage is routed to the right service, its MessageRefType is set to the 
	 * string name of the message type for messages handled by that service.
	 * 
	 * @param commandMessage
	 * @return Exception if not implemented
	 */
	public Object manage(CommandMessage commandMessage) {
		throw new UnsupportedOperationException("This adapter does not support the manage call");
	}
	
	/**
	 * Returns true if the adapter performs custom subscription management. 
	 * The default return value is false, and subclasses should override this 
	 * method as necessary.
	 * 
	 * @return true if subscriptions are handled
	 */
	public boolean handlesSubscriptions() {
		return true;
	}		
	
}
