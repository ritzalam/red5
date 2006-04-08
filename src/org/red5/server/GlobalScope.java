package org.red5.server;

import org.red5.server.api.IGlobalScope;
import org.red5.server.api.IServer;

public class GlobalScope extends Scope implements IGlobalScope {

	protected IServer server;
	
	public void setServer(IServer server) {
		this.server = server;
	}
	
	public void register() {
		server.registerGlobal(this);
		init();
	}
	
}
