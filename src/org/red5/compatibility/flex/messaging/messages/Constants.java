package org.red5.compatibility.flex.messaging.messages;

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

/**
 * Constants for the flex compatibility messages.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class Constants {

	/** Operation id of register command. */
	public static final int SUBSCRIBE_OPERATION = 0;

	public static final int UNSUBSCRIBE_OPERATION = 1;

	/** Operation id of poll command. */
	public static final int POLL_OPERATION = 2;

	/** Update given attributes from a data message. */
	public static final int DATA_OPERATION_UPDATE_ATTRIBUTES = 3;

	public static final int CLIENT_SYNC_OPERATION = 4;

	/** Operation id of ping commands. */
	public static final int CLIENT_PING_OPERATION = 5;

	/** Update destination based on nested DataMessage packet. */
	public static final int DATA_OPERATION_UPDATE = 7;

	public static final int CLUSTER_REQUEST_OPERATION = 7;

	/** Operation id of authentication commands. */
	public static final int LOGIN_OPERATION = 8;

	public static final int LOGOUT_OPERATION = 9;

	/** Set all attributes from a data message. */
	public static final int DATA_OPERATION_SET = 10;

	public static final int SUBSCRIPTION_INVALIDATE_OPERATION = 10;

	public static final int MULTI_SUBSCRIBE_OPERATION = 11;

	public static final int UNKNOWN_OPERATION = 10000;

	public static final String NEEDS_CONFIG_HEADER = "DSNeedsConfig";

	public static final String POLL_WAIT_HEADER = "DSPollWait";

	public static final String PRESERVE_DURABLE_HEADER = "DSPreserveDurable";

	public static final String REMOVE_SUBSCRIPTIONS = "DSRemSub";

	public static final String SELECTOR_HEADER = "DSSelector";

	public static final String SUBSCRIPTION_INVALIDATED_HEADER = "DSSubscriptionInvalidated";

	public static final String SUBTOPIC_SEPARATOR = "_;_";

}
