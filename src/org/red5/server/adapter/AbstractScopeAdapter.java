package org.red5.server.adapter;

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

import org.red5.server.api.IBasicScope;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.IScope;
import org.red5.server.api.IScopeHandler;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.service.IServiceCall;

public abstract class AbstractScopeAdapter implements IScopeHandler {

	private boolean canStart = true;
	private boolean canConnect = true;
	private boolean canJoin = true;
	private boolean canCallService = true;
	private boolean canAddChildScope = true;
	private boolean canHandleEvent = true;
	
	public void setCanStart(boolean canStart) {
		this.canStart = canStart;
	}

	public void setCanCallService(boolean canCallService) {
		this.canCallService = canCallService;
	}

	public void setCanConnect(boolean canConnect) {
		this.canConnect = canConnect;
	}

	public void setJoin(boolean canJoin) {
		this.canJoin = canJoin;
	}
	
	public boolean start(IScope scope) {
		return canStart;
	}

	public void stop(IScope scope) {
		// nothing
	}

	public boolean connect(IConnection conn, IScope scope, Object[] params) {
		return canConnect;
	}
	
	public void disconnect(IConnection conn, IScope scope) {
		// nothing
	}

	public boolean join(IClient client, IScope scope) {
		return canJoin;
	}
	
	public void leave(IClient client, IScope scope){
		// nothing
	}

	public boolean serviceCall(IConnection conn, IServiceCall call) {
		return canCallService;
	}

	public boolean addChildScope(IBasicScope scope) {
		return canAddChildScope;
	}

	public void removeChildScope(IBasicScope scope) {
		// TODO Auto-generated method stub	
	}
	
	public boolean handleEvent(IEvent event){
		return canHandleEvent;
	}

}