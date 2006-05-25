package org.red5.server;

import org.red5.server.api.IGlobalScope;
import org.red5.server.api.IServer;
import org.red5.server.api.ScopeUtils;
import org.red5.server.api.persistence.IPersistenceStore;

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
			store = ScopeUtils.getPersistenceStore(this, this.persistenceClass);
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
