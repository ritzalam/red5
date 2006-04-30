package org.red5.server.so;

import static org.red5.server.api.so.ISharedObject.TYPE;

import java.util.Iterator;

import org.red5.server.api.IBasicScope;
import org.red5.server.api.IScope;
import org.red5.server.api.so.ISharedObject;
import org.red5.server.api.so.ISharedObjectService;

public class SharedObjectService
	implements ISharedObjectService {
	
	public boolean createSharedObject(IScope scope, String name, boolean persistent) {
		final IBasicScope soScope = new SharedObjectScope(scope, name, persistent);
		return scope.addChildScope(soScope);
	}

	public ISharedObject getSharedObject(IScope scope, String name) {
		return (ISharedObject) scope.getBasicScope(TYPE,name);
	}

	public Iterator<String> getSharedObjectNames(IScope scope) {
		return scope.getBasicScopeNames(TYPE);
	}

	public boolean hasSharedObject(IScope scope, String name) {
		return scope.hasChildScope(TYPE,name);
	}
	
}