package org.red5.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.api.IBasicScope;
import org.red5.server.api.IClient;
import org.red5.server.api.IClientRegistry;
import org.red5.server.api.IConnection;
import org.red5.server.api.IContext;
import org.red5.server.api.IScope;
import org.red5.server.api.IScopeHandler;
import org.red5.server.api.Red5;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.service.IServiceCall;

public class CoreHandler implements IScopeHandler {

	protected static Log log =
        LogFactory.getLog(CoreHandler.class.getName());
	
	public boolean addChildScope(IBasicScope scope) {
		return true;
	}

	public boolean connect(IConnection conn, IScope scope) {
		return connect(conn, scope, null);
	}
		
	public boolean connect(IConnection conn, IScope scope, Object[] params) {
		
		log.debug("Connect to core handler ?");
		
		String id = conn.getSessionId();
		
		// Use client registry from scope the client connected to.
		IScope connectionScope = Red5.getConnectionLocal().getScope(); 
		IClientRegistry clientRegistry = connectionScope.getContext().getClientRegistry();
		
		IClient client = clientRegistry.hasClient(id) ? 
				clientRegistry.lookupClient(id) : clientRegistry.newClient(params);
		
		// We have a context, and a client object.. time to init the conneciton.
		conn.initialize(client);
		
		// we could checked for banned clients here 
		return true;
	}

	public void disconnect(IConnection conn, IScope scope) {
		// do nothing here
	}

	public boolean join(IClient client, IScope scope) {
		return true;
	}

	public void leave(IClient client, IScope scope) {
		// do nothing here
	}

	public void removeChildScope(IBasicScope scope) {
		// do nothing here
	}

	public boolean serviceCall(IConnection conn, IServiceCall call) {
		final IContext context = conn.getScope().getContext();
		if(call.getServiceName() != null){
			context.getServiceInvoker().invoke(call, context);
		} else {
			context.getServiceInvoker().invoke(call, conn.getScope().getHandler());
		}
		return true;
	}

	public boolean start(IScope scope) {
		return true;
	}

	public void stop(IScope scope) {
		// do nothing here
	}

	public boolean handleEvent(IEvent event) {
		return false;
	}
	
}
