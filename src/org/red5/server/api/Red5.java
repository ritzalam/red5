package org.red5.server.api;

import org.red5.server.context.AppContext;

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
	
	public static AppContext getContext(){
		final Connection conn = getConnection();
		if(conn == null) return null;
		else return conn.getApplication().getContext();
	}
	
}
