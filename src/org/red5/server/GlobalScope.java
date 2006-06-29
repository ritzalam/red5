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

import org.red5.server.api.IGlobalScope;
import org.red5.server.api.IServer;
import org.red5.server.api.persistence.IPersistenceStore;
import org.red5.server.api.persistence.PersistenceUtils;

public class GlobalScope extends Scope implements IGlobalScope {

	protected IServer server;
	
	public void setPersistenceClass(String persistenceClass) throws Exception {
		this.persistenceClass = persistenceClass;
		// We'll have to wait for creation of the store object
		// until all classes have been initialized.
	}
	
	public IPersistenceStore getStore() {
		if (store != null)
			return store;
		
		try {
			store = PersistenceUtils.getPersistenceStore(this, this.persistenceClass);
		} catch (Exception error) {
			log.error("Could not create persistence store.", error);
			store = null;
		}
		return store;
	}
	
	
	public void setServer(IServer server) {
		this.server = server;
	}
	
	public void register() {
		server.registerGlobal(this);
		init();
	}
	
}
