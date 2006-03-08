package org.red5.server.api;

// The client object is unique to the host
// It provides access to the host session
// The client object will be stored http session if its available

public interface Client extends AttributeStore {

	public static final String HTTP_SESSION_KEY = "red5.client";
	
	// Unique client id
	public String getId();
	public String getHost();
	public long getCreationTime();

	// Only one connection is allowed per client, per scope
	public Connection getConnection(Scope scope);
	
	// NOTE: I removed session, since client serves the same purpose
}