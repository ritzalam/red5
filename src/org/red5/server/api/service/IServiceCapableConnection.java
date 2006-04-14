package org.red5.server.api.service;

// TODO: this should really extend IServiceInvoker
public interface IServiceCapableConnection {
	
	public IServiceCall invoke(IServiceCall call);

}
