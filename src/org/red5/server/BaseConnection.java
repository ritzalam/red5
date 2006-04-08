package org.red5.server;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.api.IBasicScope;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.IScope;
import org.red5.server.api.event.IEvent;

public class BaseConnection extends AttributeStore 
	implements IConnection {
	
	protected static Log log =
        LogFactory.getLog(BaseConnection.class.getName());
	
	protected String type;
	protected String host;
	protected String path;
	protected String sessionId;
	protected Map<String,String> params = null;
	
	protected IClient client = null;
	protected Scope scope = null;
	protected Set<IBasicScope> basicScopes;
	
	public BaseConnection(String type, String host, String path, String sessionId, Map<String,String> params){
		this.type = type;
		this.host = host;
		this.path = path;
		this.sessionId = sessionId;
		this.params = params;
		this.basicScopes = new HashSet<IBasicScope>();
	}
	
	public void initialize(IClient client){
		this.client = client;
	}
	
	public String getType(){
		return type;
	}

	public String getHost() {
		return host;
	}

	public String getPath(){
		return path;
	}

	public String getSessionId() {
		return sessionId;
	}

	public Map<String,String> getParams(){
		return params;
	}

	public IClient getClient() {
		return client;
	}

	public boolean isConnected(){
		return scope != null;
	}

	public boolean connect(IScope newScope) {
		final Scope oldScope = scope;
		scope = (Scope) newScope;
		if(scope.connect(this)){
			if(oldScope != null) 
				oldScope.disconnect(this);
			return true;
		} else 	{
			scope = oldScope;
			return false;
		}
	}

	public IScope getScope() {
		return scope;
	}
	
	public void close(){
		if(isConnected()) {
			log.debug("Close, disconnect from scope");
			scope.disconnect(this);
			scope=null;
		} else {
			log.debug("Close, not connected nothing to do.");
		}
	}

	public void notifyEvent(IEvent event) {
		// TODO Auto-generated method stub		
	}

	public void dispatchEvent(Object event){
		// wrap as IEvent and forward
	}

	public void dispatchEvent(IEvent event) {
		
	}

	public boolean handleEvent(IEvent event) {
		return getScope().handleEvent(event);
	}

	public Iterator<IBasicScope> getBasicScopes() {
		return basicScopes.iterator();
	}
	
	
	/* This is really a utility
	public boolean switchScope(String contextPath) {
		// At the moment this method is not dealing with tree schematics
		Scope newScope = (Scope) ScopeUtils.resolveScope(scope, contextPath);
		if(newScope == null) return false;
		return connect(scope);
	}
	*/
	
}