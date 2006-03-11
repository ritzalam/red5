package org.red5.server.api;

import org.springframework.context.ApplicationContext;

/**
 * Utility class for accessing red5 api objects 
 * 
 * This class uses a thread local, and will be setup by the service invoker
 * 
 * The code below shows various uses.
 * <p>
 * <code>
 * Connection conn = Red5.getConnectionLocal();
 * Red5 r5 = new Red5();
 * Scope scope = r5.getScope();
 * conn = r5.getConnection();
 * r5 = new Red5(conn);
 * Client client = r5.getClient();
 * </code>
 * </p>
 */
public class Red5 {

	private static ThreadLocal connThreadLocal = new ThreadLocal();

	public Connection conn = null;

	/**
	 * Create a new Red5 object using given connection 
	 * @param conn
	 * 	connection object
	 */
	public Red5(Connection conn) {
		this.conn = conn;
	}

	/**
	 * Create a new Red5 object using the connection local to the current thread
	 * A bit of magic that lets you access the red5 scope from anywhere
	 */
	public Red5() {
		conn = Red5.getConnectionLocal();
	}

	static void setConnectionLocal(Connection connection) {
		connThreadLocal.set(connection);
	}

	/** 
	 * Get the connection associated with the current thread
	 * 
	 * @return connection object
	 */
	public static Connection getConnectionLocal() {
		return (Connection) connThreadLocal.get();
	}

	/**
	 * Get the connection object
	 * 
	 * @return connection object
	 */
	public Connection getConnection() {
		return conn;
	}

	/** 
	 * Get the scope
	 * 
	 * @return scope object
	 */
	public Scope getScope() {
		return conn.getScope();
	}

	/**
	 * Get the client
	 * 
	 * @return	client object
	 */
	public Client getClient() {
		return conn.getClient();
	}

	/**
	 * Get the spring application context
	 * 
	 * @return	application context
	 */
	public ApplicationContext getContext() {
		return conn.getScope().getContext();
	}

}