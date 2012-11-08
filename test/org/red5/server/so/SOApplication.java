package org.red5.server.so;

import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.so.ISharedObject;

public class SOApplication extends MultiThreadedApplicationAdapter {

	private String persistentSO = "persistentSO";
	
	/* (non-Javadoc)
	 * @see org.red5.server.adapter.MultiThreadedApplicationAdapter#appStart(org.red5.server.api.scope.IScope)
	 */
	@SuppressWarnings("unused")
	@Override
	public boolean appStart(IScope app) {		
		// create persistent SO
		createSharedObject(app, persistentSO, true);
		// get the SO
		ISharedObject sharedObject = getSharedObject(app, persistentSO, true);
		//
		return super.appStart(app);
	}

	/* (non-Javadoc)
	 * @see org.red5.server.adapter.MultiThreadedApplicationAdapter#appStop(org.red5.server.api.scope.IScope)
	 */
	@Override
	public void appStop(IScope app) {
		super.appStop(app);
	}

	/* (non-Javadoc)
	 * @see org.red5.server.adapter.MultiThreadedApplicationAdapter#roomStart(org.red5.server.api.scope.IScope)
	 */
	@Override
	public boolean roomStart(IScope room) {
		getSharedObject(scope, "persistentSO", true);
		return super.roomStart(room);
	}

	/* (non-Javadoc)
	 * @see org.red5.server.adapter.MultiThreadedApplicationAdapter#roomStop(org.red5.server.api.scope.IScope)
	 */
	@Override
	public void roomStop(IScope room) {
		super.roomStop(room);
	}

	/* (non-Javadoc)
	 * @see org.red5.server.adapter.MultiThreadedApplicationAdapter#appConnect(org.red5.server.api.IConnection, java.lang.Object[])
	 */
	@Override
	public boolean appConnect(IConnection conn, Object[] params) {
		return super.appConnect(conn, params);
	}

	/* (non-Javadoc)
	 * @see org.red5.server.adapter.MultiThreadedApplicationAdapter#roomConnect(org.red5.server.api.IConnection, java.lang.Object[])
	 */
	@Override
	public boolean roomConnect(IConnection conn, Object[] params) {
		return super.roomConnect(conn, params);
	}

	/* (non-Javadoc)
	 * @see org.red5.server.adapter.MultiThreadedApplicationAdapter#appDisconnect(org.red5.server.api.IConnection)
	 */
	@Override
	public void appDisconnect(IConnection conn) {
		super.appDisconnect(conn);
	}

	/* (non-Javadoc)
	 * @see org.red5.server.adapter.MultiThreadedApplicationAdapter#roomDisconnect(org.red5.server.api.IConnection)
	 */
	@Override
	public void roomDisconnect(IConnection conn) {
		super.roomDisconnect(conn);
	}

	/* (non-Javadoc)
	 * @see org.red5.server.adapter.MultiThreadedApplicationAdapter#roomJoin(org.red5.server.api.IClient, org.red5.server.api.scope.IScope)
	 */
	@Override
	public boolean roomJoin(IClient client, IScope room) {
		return super.roomJoin(client, room);
	}

	/* (non-Javadoc)
	 * @see org.red5.server.adapter.MultiThreadedApplicationAdapter#roomLeave(org.red5.server.api.IClient, org.red5.server.api.scope.IScope)
	 */
	@Override
	public void roomLeave(IClient client, IScope room) {
		super.roomLeave(client, room);
	}

}
