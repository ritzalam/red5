package org.red5.demos.fitc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.adapter.ApplicationAdapter;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.IScope;
import org.red5.server.api.Red5;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;
import org.red5.server.api.service.IServiceCapableConnection;

public class Application extends ApplicationAdapter implements IPendingServiceCallback {

	private static final Log log = LogFactory.getLog(Application.class);
	
	private final static String CLIENT_ID = "fitc.ClientId";
	
	public boolean start(IScope scope) {
		if (!super.start(scope))
			return false;
		
		// Initialize id generation
		synchronized (scope) {
			scope.setAttribute(CLIENT_ID, 0);
		}
		return true;
	}
	
	public boolean appJoin(IClient client, IScope app) {
		if (!super.appJoin(client, app))
			return false;
		
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
			
			Object[] args = new Integer[1];
			args[0] = id;
			service.invoke("setId", args, this);
		}
		
		return true;
	}

	public void resultReceived(IPendingServiceCall call) { 
		log.info("Received result: " + call.getResult());
	}
	
};
