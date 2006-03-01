package org.red5.server.api;

import org.springframework.context.ApplicationContext;

/**
 * Utility class for accessing red5 api objects 
 * This class uses a thread local, and will be setup by the service invoker
 * 
 * The most important method is..
 * Red5.getConnectionLocal() // get the current connection
 * 
 * The other methods are just short cuts
 * 
 * Red5 r5 = new Red5();
 * r5.getClient() // get the current client object
 * r5.getContext() // get the spring app conext
 * r5.getScope() // get the current application scope
 * r5.getSession() // get the current host session
 *  
 */
public class Red5 {

	private static ThreadLocal connThreadLocal = new ThreadLocal();
	public Connection conn = null;
	
	public Red5(Connection conn){
		this.conn = conn;
	}
	
	public Red5(){
		conn = Red5.getConnectionLocal();
	}
	
	// Package level static method to set the connection local to the current thread
	static void setConnectionLocal(Connection connection){
		connThreadLocal.set(connection);
	}
	
	static Connection getConnectionLocal(){
		return (Connection) connThreadLocal.get();
	}

	// Public API Methods
	
	// The conneciton is the root
	public Connection getConnection(){
		return conn;
	}
	
	// The rest of the methods are just shortcuts
	public Scope getScope(){
		if(conn == null) return null;
		else return conn.getScope();
	}
	
	public Client getClient(){
		if(conn == null) return null;
		else return conn.getClient();
	}
	
	public Session getSession(){
		if(conn == null) return null;
		else return conn.getClient().getSession();
	}
	
	public  ApplicationContext getContext(){
		if(conn == null) return null;
		else return conn.getScope().getContext();
	}
	
}