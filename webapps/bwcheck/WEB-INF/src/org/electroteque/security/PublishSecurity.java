package org.electroteque.security;

import org.red5.server.api.IScope;
import org.red5.server.api.stream.IStreamPublishSecurity;

public class PublishSecurity implements IStreamPublishSecurity {
    
	public boolean isPublishAllowed(IScope scope, String name, String mode) {
		return false;
    }
    
}