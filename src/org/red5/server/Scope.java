package org.red5.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.api.IBasicScope;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.IContext;
import org.red5.server.api.IScope;
import org.red5.server.api.IScopeAware;
import org.red5.server.api.IScopeHandler;
import org.red5.server.api.event.IEvent;
import org.springframework.core.io.Resource;
import org.springframework.core.style.ToStringCreator;

public class Scope extends BasicScope implements IScope {
	
	protected static Log log =
        LogFactory.getLog(Scope.class.getName());
	
	private static final int UNSET = -1;
	private static final String TYPE = "scope";
	
	private int depth = UNSET; 
	private IContext context;
	private IScopeHandler handler;
	
	private boolean autoStart = true;
	private boolean enabled = true;
	private boolean running = false;
	
	private HashMap<String,IBasicScope> children = new HashMap<String,IBasicScope>();
	private HashMap<IClient,Set<IConnection>> clients = new HashMap<IClient,Set<IConnection>>();
	
	public Scope(){
		this(null);
	}
	
	public Scope(String name){
		super(null,TYPE,name,false);
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isRunning() {
		return running;
	}

	public void setAutoStart(boolean autoStart) {
		this.autoStart = autoStart;
	}

	public void setContext(IContext context) {
		this.context = context;
	}

	public void setHandler(IScopeHandler handler) {
		this.handler = handler;
		if(handler instanceof IScopeAware){
			((IScopeAware) handler).setScope(this);
		} 
	}

	public void init(){
		if(hasParent()){
			if(!parent.hasChildScope(name)){
				if(!parent.addChildScope(this)) return;
			}
		}
		if(autoStart) start();
	}
	
	public boolean start(){
		if(enabled && !running){
			if(hasHandler() && !handler.start(this)) return false;
			else return true;
		} else return false;
	}
	
	public void stop(){
		if(running){
			
		}
	}
	
	public void destory(){
		if(hasParent()) parent.removeChildScope(this);
		if(hasHandler()) handler.stop(this);
		// TODO:  kill all child scopes
	}

	
	public boolean addChildScope(IBasicScope scope){
		if(hasHandler() && !getHandler().addChildScope(scope)) {
			log.debug("Failed to add child scope: "+scope+" to "+this);
			return false;
		}
		if(scope instanceof IScope){
			// start the scope
			if (hasHandler() && !getHandler().start((IScope) scope)) {
				log.debug("Failed to start child scope: "+scope+" in "+this);
				return false;
			}
		}
		log.debug("Add child scope: "+scope+" to "+this);
		children.put(scope.getType() + SEPARATOR + scope.getName(),scope);
		return true;
	} 
	
	public void setChildLoadPath(String pattern){
		
	}
	
	public void removeChildScope(IBasicScope scope){
		if (scope instanceof IScope) {
			if (hasHandler())
				getHandler().stop((IScope) scope);
		}
		children.remove(scope);
	}
	
	public boolean hasChildScope(String name){
		log.debug("Has child scope? "+name+" in "+this);
		return children.containsKey(TYPE + SEPARATOR + name);
	}
	
	public boolean hasChildScope(String type, String name){
		return children.containsKey(type + SEPARATOR + name);
	}
	
	public Iterator<String> getScopeNames() {
		return new PrefixFilteringStringIterator(children.keySet().iterator(),"scope");
	}

	public Set<IClient> getClients() {
		return clients.keySet();
	}

	public boolean hasContext(){
		return context != null;
	}
	
	public IContext getContext() {
		if( ! hasContext() && hasParent()) {
			log.debug("returning parent context");
			return parent.getContext();
		}
		else {
			log.debug("returning context");
			return context;
		}
	}

	public String getContextPath(){
		if(hasContext()) return "";
		else if(hasParent()) return parent.getContextPath() + "/" + name;
		else return null;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	public String getPath() {
		if(hasParent()) return parent.getPath() + "/" + parent.getName();
		else return "";
	}

	public void setParent(IScope parent){
		this.parent = parent;
	}
	
	public boolean hasHandler() {
		return (handler != null || (hasParent() && getParent().hasHandler()));
	}
	
	public IScopeHandler getHandler() {
		if(handler != null) return handler;
		else if(hasParent()) return getParent().getHandler();
		else return null;
	}
		
	public IScope getParent(){
		return parent;
	}

	public boolean hasParent() {
		return (parent != null);
	}

	public synchronized boolean connect(IConnection conn) {
		return connect(conn, null);
	}
	
	public synchronized boolean connect(IConnection conn, Object[] params) {
	   //log.debug("Connect: "+conn+" to "+this);
	   //log.debug("has handler? "+hasHandler());
	   if(hasParent() && !parent.connect(conn, params)) return false;
	   if(hasHandler() && !getHandler().connect(conn, this, params)) return false;
	   final IClient client = conn.getClient();
	   //log.debug("connected to: "+this);
	   if(!clients.containsKey(client)){
		    //log.debug("Joining: "+this);
		    if(hasHandler() && !getHandler().join(client, this)) return false;
			final Set<IConnection> conns = new HashSet<IConnection>();
			conns.add(conn);
			clients.put(conn.getClient(), conns);
		} else {
			
			final Set<IConnection> conns = clients.get(client);
			conns.add(conn);
		}
	   addEventListener(conn);
	   return true;
	}
	
	public synchronized void disconnect(IConnection conn){
		if(hasParent()) parent.disconnect(conn);
		final IClient client = conn.getClient();
		if(clients.containsKey(client)){
			final Set conns = clients.get(client);
			conns.remove(conn);
			removeEventListener(conn);
			if(hasHandler()) 
				handler.disconnect(conn, this);
			if(conns.isEmpty()) {
				clients.remove(client);
				if(hasHandler()){
					// there may be a timeout here ?
					handler.leave(client, this);
				}
			}
		}
	}

	public void setDepth(int depth){
		this.depth  = depth;
	}
	
	public int getDepth() {
		if(depth == UNSET ){
			if(hasParent()){
				depth = parent.getDepth() + 1;
			} else {
				depth = 0;
			}
		} 
		return depth;
	}

	public Resource[] getResources(String path) throws IOException {
		if(hasContext()) return context.getResources(path);
		return getContext().getResources(getContextPath() + "/" + path);
	}

	public Resource getResource(String path) {
		if(hasContext()) return context.getResource(path);
		return getContext().getResource(getContextPath() + "/" + path);
	}
	
	public Iterator<IConnection> getConnections() {
		return new ConnectionIterator();
	}

	public Set<IConnection> lookupConnections(IClient client) {
		return clients.get(client);
	}

	public void dispatchEvent(IEvent event) {
		Iterator<IConnection> conns = getConnections();
		while(conns.hasNext()){
			try {
				conns.next().dispatchEvent(event);
			} catch (RuntimeException e) {
				log.error(e);
			}
		}
	}
	
	public void dispatchEvent(Object event){
		// wrap then forward
	}
	
	class PrefixFilteringStringIterator implements Iterator<String>{

		private Iterator<String> iterator; 
		private String prefix;
		private String next;
		
		public PrefixFilteringStringIterator(Iterator<String> iterator, String prefix){
			this.iterator = iterator;
			this.prefix = prefix;
		}
		
		public boolean hasNext() {
			if(next != null) return true;
			do {
				next = (iterator.hasNext()) ? iterator.next() : null;
			} while (next != null && !next.startsWith(prefix));
			return next != null;
		}

		public String next(){
			if(next != null){
				final String result = next;
				next = null;
				return result;
			}
			if(hasNext()) return next();
			else return null;
		}

		public void remove() {
			// not possible
		}
		
	}
	
	class ConnectionIterator implements Iterator<IConnection> {
		
		private Iterator setIterator; 
		private Iterator connIterator = null;
		private IConnection current = null;
		
		public ConnectionIterator(){
			setIterator = clients.values().iterator();
		}
		
		public boolean hasNext() {
			return (connIterator != null && connIterator.hasNext()) || setIterator.hasNext();
		}

		public IConnection next() {
			if (connIterator == null || !connIterator.hasNext()){
				if(!setIterator.hasNext()) return null;
				connIterator = ((Set) setIterator.next()).iterator();
			}
			current = (IConnection) connIterator.next();
			return current;
		}

		public void remove() {
			if(current!=null){
				disconnect(current);
			}
		}
		
	}

	public boolean createChildScope(String name){
		final Scope scope = new Scope(name);
		scope.setParent(this);
		return addChildScope(scope);
	}
	

	public boolean handleEvent(IEvent event) {
		// TODO Auto-generated method stub
		return false;
	}

	public IBasicScope getBasicScope(String type, String name) {
		return children.get(type+SEPARATOR+name);
	}

	public Iterator<String> getBasicScopeNames(String type) {
		if(type == null) return children.keySet().iterator();
		else return new PrefixFilteringStringIterator(children.keySet().iterator(), type+SEPARATOR);
	}

	public IScope getScope(String name){
		return (IScope) children.get(TYPE+SEPARATOR+name);
	}

	@Override
	public Iterator<IBasicScope> iterator() {
		return children.values().iterator();
	}

	public String toString(){
		final ToStringCreator tsc = new ToStringCreator(this);
		return tsc.append("Depth",getDepth())
			.append("Path",getPath())
			.append("Name",getName())
			.toString();
	}
	
}