package org.red5.server;

import org.red5.server.api.IGlobalScope;
import org.red5.server.api.IScope;
import org.red5.server.api.IScopeResolver;
import org.red5.server.api.ScopeUtils;
import org.red5.server.exception.ScopeNotFoundException;

public class ScopeResolver implements IScopeResolver {

	public static final String DEFAULT_HOST = "";
	
	protected IGlobalScope globalScope;

	public IGlobalScope getGlobalScope() {
		return globalScope;
	}

	public void setGlobalScope(IGlobalScope root) {
		this.globalScope = root;
	}

	public IScope resolveScope(String path){
		IScope scope = globalScope;
		if(path == null) return scope;
		final String[] parts = path.split("/");
		for(int i=0; i < parts.length; i++){
			final String room = parts[i];
			if(scope.hasChildScope(room)){
				scope = scope.getScope(room);
			} else if(scope.createChildScope(room)){
				scope = scope.getScope(room);
			} else throw new ScopeNotFoundException(scope,parts[i]);
		}
		return scope;
	}
	
}
