package org.red5.server.api;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 *
 * Copyright (c) 2006-2007 by respective authors (see below). All rights reserved.
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

import java.util.Collection;
import java.util.Set;

/**
 * The client object represents a single client. One client may have multiple
 * connections to different scopes on the same host. In some ways the client
 * object is like a HTTP session. You can create IClient objects with
 * {@link IClientRegistry#newClient(Object[])}
 *
 *
 * NOTE: I removed session, since client serves the same purpose as a client
 * with attributes
 *
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard (luke@codegent.com)
 */
public interface ClientMBean {

	public String getId();

	public long getCreationTime();

	public Collection<IScope> getScopes();

	public Set<IConnection> getConnections();

	public void disconnect();

}