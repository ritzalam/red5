package org.red5.server.stream;

import org.red5.server.api.IScope;
import org.red5.server.messaging.IProvider;

public interface IProviderService {
	public static final String KEY = "providerService";
	
	IProvider getProvider(IScope scope, String name);
	
	
}
