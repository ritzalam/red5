package org.red5.server.api.test;

public class JettyTest {

	public void startJettyAndCheckContext() {
		// load spring
		// -> load the global contexts
		// --> setup mina, handlers, etc
		// --> create global scopes
		// create jetty server
		// --> configure using xml
		// ---> load the webapps contexts
		// inspect the contexts in jetty server
		// -> set the correct global context
		// -> set aliases on global. these are the hosts.
		// start jetty server
		// -> start the webapp
		// --> listener loads spring & connects via global context
		// our work is done 
	}

}
