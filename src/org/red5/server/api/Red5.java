package org.red5.server.api;

import org.red5.server.context.AppContext;

/**
 * Utility class for accessing red5 api objects 
 * This class uses a thread local, and will be setup by the service invoker
 * 
 * The most important method is..
 * Red5.getConnection() // get the current connection
 * 
 * The other methods are just short cuts
 * 
 * Red5.getClient() // get the current client object
 * Red5.getContext() // get the spring app conext
 * Red5.getApplication() // get the applicaiton object
 * Red5.getScope() // get the current application scope
 * Red5.getSession() // get the current host session
 *  
 */
public class Red5 {

	private static ThreadLocal connThreadLocal = new ThreadLocal();
	
	public static void setConnection(Connection connection){
		connThreadLocal.set(connection);
	}
	
	public static Connection getConnection(){
		return (Connection) connThreadLocal.get();
	}
	
	public static Application getApplication(){
		final Connection conn = getConnection();
		if(conn == null) return null;
		else return conn.getApplication();
	}
	
	public static Scope getScope(){
		final Connection conn = getConnection();
		if(conn == null) return null;
		else return conn.getScope();
	}
	
	public static Client getClient(){
		final Connection conn = getConnection();
		if(conn == null) return null;
		else return conn.getClient();
	}
	
	public static Session getSession(){
		final Connection conn = getConnection();
		if(conn == null) return null;
		else return conn.getClient().getSession();
	}
	
	public static AppContext getContext(){
		final Connection conn = getConnection();
		if(conn == null) return null;
		else return conn.getApplication().getContext();
	}
	
}
