package org.red5.demos.fitc;

import java.util.Iterator;

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
import org.red5.server.api.stream.IStreamAware;

public class Application extends ApplicationAdapter implements IPendingServiceCallback, IStreamAware {

	private static final Log log = LogFactory.getLog(Application.class);
	
	@Override
	public boolean appStart(IScope scope) {
		// init your handler here
		return true;
	}

	@Override
	public boolean appConnect(IConnection conn, Object[] params) {
		IServiceCapableConnection service = (IServiceCapableConnection) conn;
		log.info("Client connected " + conn.getClient().getId() + " conn " + conn);
		log.info("Setting stream id: "+ getClients().size()); // just a unique number
		service.invoke("setId", new Object[]{conn.getClient().getId()}, this);
		return true;
	}

	@Override
	public boolean appJoin(IClient client, IScope scope) {
		log.info("Client joined app " + client.getId());
		// If you need the connecion object you can access it via.
		IConnection conn = Red5.getConnectionLocal();
		return true;
	}
	
	public void streamBroadcastStart(String name) {
		// Notify all the clients that the stream had been started
		log.debug("stream broadcast start: "+name);
		IConnection current = Red5.getConnectionLocal();
		IScope parent = scope.getParent();
		Iterator<IConnection> it = parent.getConnections();
		while (it.hasNext()) {
			IConnection conn = it.next();
			if (conn.equals(current))
				// Don't notify current client
				continue;
			
			if (conn instanceof IServiceCapableConnection)
				((IServiceCapableConnection) conn).invoke("newStream", new Object[]{name}, this);
		}
	}
	
	/**
	 * Get streams. called from client
	 * @return iterator of broadcast stream names
	 */
	public Iterator<String> getStreams(){
		return getBroadcastStreamNames();
	}
	
	/**
	 * Handle callback from service call. 
	 */
	public void resultReceived(IPendingServiceCall call) { 
		log.info("Received result " + call.getResult() + " for " + call.getServiceMethodName());
	}
	
}