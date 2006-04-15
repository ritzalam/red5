package org.red5.demos.fitc;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.adapter.ApplicationAdapter;
import org.red5.server.api.IBasicScope;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.IScope;
import org.red5.server.api.Red5;
import org.red5.server.api.ScopeUtils;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;
import org.red5.server.api.service.IServiceCapableConnection;
import org.red5.server.api.stream.IBroadcastStream;

public class Application extends ApplicationAdapter implements IPendingServiceCallback {

	private static final Log log = LogFactory.getLog(Application.class);
	
	private final static String CLIENT_ID = "fitc.ClientId";
	private final static String STREAMS_ID = "fitc.Streams";
	
	public boolean start(IScope scope) {
		if (!super.start(scope))
			return false;
		
		// Initialize id generation and list of streams
		if (ScopeUtils.isApp(scope)) {
			synchronized (scope) {
				scope.setAttribute(CLIENT_ID, 0);
			}
		}
		
		// Every scope keeps a list of connected streams
		synchronized (scope) {
			scope.setAttribute(STREAMS_ID, new HashSet<String>());
		}
		return true;
	}
	
	public boolean join(IClient client, IScope scope) {
		if (!super.join(client, scope))
			return false;
		
		// Only set the id for the application
		if (!ScopeUtils.isApp(scope))
			return true;
		
		Integer id;
		if (!client.hasAttribute(CLIENT_ID)) {
			// Generate new client id
			synchronized (scope) {
				id = (Integer) scope.getAttribute(CLIENT_ID);
				scope.setAttribute(CLIENT_ID, id + 1);
			}
			
			// Store locally for future use
			client.setAttribute(CLIENT_ID, id);
		} else {
			// Returning client
			id = (Integer) client.getAttribute(CLIENT_ID);
		}
		
		// Send to client
		IConnection conn = Red5.getConnectionLocal();
		if (conn instanceof IServiceCapableConnection) {
			IServiceCapableConnection service = (IServiceCapableConnection) conn;
			
			log.info("Setting id " + id + " for client " + client);
			service.invoke("setId", new Object[]{id}, this);
		}
		
		return true;
	}

	public void resultReceived(IPendingServiceCall call) { 
		log.info("Received result: " + call.getResult());
	}
	
	public boolean addChildScope(IBasicScope scope) {
		if (!super.addChildScope(scope))
			return false;
		
		if (scope instanceof IBroadcastStream) {
			IConnection current = Red5.getConnectionLocal();
			IScope parent = scope.getParent();
			Iterator<IConnection> it = parent.getConnections();
			while (it.hasNext()) {
				IConnection conn = it.next();
				if (conn.equals(current))
					// Don't notify current client
					continue;
				
				if (conn instanceof IServiceCapableConnection)
					((IServiceCapableConnection) conn).invoke("newStream", new Object[]{scope.getName()}, this);
			}
			
			synchronized (parent) {
				Set<String> streams = (Set<String>) parent.getAttribute(STREAMS_ID);
				streams.add(scope.getName());
			}
		}
		return true;
	}
	
	public void removeChildScope(IBasicScope scope) {
		if (scope instanceof IBroadcastStream) {
			IScope parent = scope.getParent();
			synchronized (parent) {
				Set<String> streams = (Set<String>) parent.getAttribute(STREAMS_ID);
				streams.remove(scope.getName());
			}
		}
		
		super.removeChildScope(scope);
	}
	
	synchronized public Set<String> getStreams() {
		// Get list of streams from curent scope
		IScope scope = Red5.getConnectionLocal().getScope();
		log.info("Getting streams from " + scope);
		Set<String> streams = (Set<String>) scope.getAttribute(STREAMS_ID);
		return streams; 
	}
};
