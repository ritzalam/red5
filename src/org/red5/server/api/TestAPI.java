package org.red5.server.api;

public class TestAPI {

	public void test(){
		// This class doesnt do anything its just me playing around with the api.
		
		// Assume the rtmp connection string looks like this
		// nc.connect("rtmp://myhost/chatApp/lobby");
		
		// This would be called internally inside red5
		Connection conn = null;
		Red5.setConnectionLocal(conn);
		
		// The api could then be accessed from anywhere (withing same thread)
		Red5 r5 = new Red5();
	
		r5.getClient().getId(); // return the clients id
		
		// Return the app context 
		r5.getContext();
		
		// Get a app root level file
		r5.getContext().getResource("app.xml");
		
		// Get a file relative to the scope path
		r5.getScope().getResource("room.xml");
		
		// Dispatch an event to a connection
		r5.getConnection().dispatchEvent("hello");
	
	
	}
	
}
