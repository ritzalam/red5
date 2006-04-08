package org.red5.server.so;

import static org.red5.server.api.so.ISharedObject.TYPE;

import java.util.Iterator;

import org.red5.server.api.IBasicScope;
import org.red5.server.api.IScope;
import org.red5.server.api.so.ISharedObject;
import org.red5.server.api.so.ISharedObjectService;

public class ScopeWrappingSharedObjectService
	implements ISharedObjectService {
	
	protected IScope scope;
	
	public ScopeWrappingSharedObjectService(IScope scope){
		this.scope = scope;
	}
	
	public boolean createSharedObject(String name, boolean persistent) {
		final IBasicScope soScope = new SharedObjectScope(scope,name,persistent);
		return scope.addChildScope(soScope);
	}

	public ISharedObject getSharedObject(String name) {
		return (ISharedObject) scope.getBasicScope(TYPE,name);
	}

	public Iterator<String> getSharedObjectNames() {
		return scope.getBasicScopeNames(TYPE);
	}

	public boolean hasSharedObject(String name) {
		return scope.hasChildScope(TYPE,name);
	}
	
}