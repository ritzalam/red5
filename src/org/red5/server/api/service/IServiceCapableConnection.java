package org.red5.server.api.service;

// TODO: this should really extend IServiceInvoker
public interface IServiceCapableConnection {
	
	public void invoke(IServiceCall call);
	public void invoke(String method);
	public void invoke(String method, IPendingServiceCallback callback);
	public void invoke(String method, Object[] params);
	public void invoke(String method, Object[] params, IPendingServiceCallback callback);

}
