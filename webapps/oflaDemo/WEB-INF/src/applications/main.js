/*
 * main.js - a translation into JavaScript of the olfa demo Application class, a red5 example.
 *
 * @author Paul Gregoire
 */

importPackage(Packages.org.red5.server.api);
importPackage(Packages.org.red5.server.api.stream);
importPackage(Packages.org.red5.server.api.stream.support);
importPackage(Packages.org.apache.commons.logging);

importClass(Packages.org.springframework.core.io.Resource);
importClass(Packages.org.red5.server.api.Red5);
importClass(Packages.org.red5.server.api.IScopeHandler);

var IStreamCapableConnection = Packages.org.red5.server.api.stream.IStreamCapableConnection;

function object(o) {
	function Application() {
		this.appScope;
		this.serverStream;
		this.className = 'Application';
	  	this.testMe = function() {
			print('Javascript testMe');
		};
	
		this.getClassName = function() {
			return this.className;
		};
		
		appStart: function(app) {
			print('Javascript appStart');
			this.appScope = app;
			return true;
		};   
	
		appConnect: function(conn, params) {
			print('Javascript appConnect');
			measureBandwidth(conn);
			if (conn == typeof(IStreamCapableConnection)) {
				var streamConn = conn;
				var sbc = new Packages.org.red5.server.api.stream.support.SimpleBandwidthConfigure();
				sbc.setMaxBurst(8388608);
				sbc.setBurst(8388608);
				sbc.setOverallBandwidth(2097152);
				streamConn.setBandwidthConfigure(sbc);
			}
			return this.__proto__.appConnect(conn, params);
		};
		
		appDisconnect: function(conn) {
			print('Javascript appDisconnect');
			if (this.appScope == conn.getScope() && this.serverStream)  {
				this.serverStream.close();
			}
			return this.__proto__.appDisconnect(conn);
		};  			
	}
	Application.prototype = o;
	return new Application();
}

//if a super class exists in the namespace / bindings
if (supa) {
	print('New instance of prototype: ' + supa);
	//print('Result: ' + object(supa));
	object(supa);
}







