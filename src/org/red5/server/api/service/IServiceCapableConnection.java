package org.red5.server.api.service;

import org.red5.server.api.IConnection;

// TODO: this should really extend IServiceInvoker
public interface IServiceCapableConnection extends IConnection {
	
	public void invoke(IServiceCall call);
	public void invoke(String method);
	public void invoke(String method, IPendingServiceCallback callback);
	public void invoke(String method, Object[] params);
	public void invoke(String method, Object[] params, IPendingServiceCallback callback);

}
