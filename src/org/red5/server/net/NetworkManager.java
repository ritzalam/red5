package org.red5.server.net;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright © 2006 by respective authors. All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either version 2.1 of the License, or (at your option) any later 
 * version. 
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this library; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 * @author Dominick Accattato (daccattato@gmail.com)
 */

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.TransportType;
import org.apache.mina.io.IoHandlerAdapter;
import org.apache.mina.protocol.ProtocolProvider;
import org.apache.mina.registry.Service;
import org.apache.mina.registry.ServiceRegistry;
import org.apache.mina.registry.SimpleServiceRegistry;

/**
 * The NetworkManager class represents the servers network status.  
 * The server can be started by calling networkManager.up() {@link org.red5.server.Server.java}
 * The server can be brought down by calling networkManager.down();
 * Last, the server can be extended through Mina to bind other protocols through this class.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 * @author Dominick Accattato (daccattato@gmail.com)
 * @version 0.3
 */
public class NetworkManager {

	public static final byte STATUS_NETWORK_UP = 0x00;
	public static final byte STATUS_NETWORK_DOWN = 0x01;
	public static final byte STATUS_NETWORK_ERROR = 0x02;
	private static final boolean USE_SSL = false;
	
	// Initialize Logging
	protected static Log log =
        LogFactory.getLog(NetworkManager.class.getName());

	protected byte networkStatus = STATUS_NETWORK_DOWN;
	protected Map serviceConfig;
	protected ServiceRegistry registry;
	
	/**
	 * NetworkManager Constructor
	 * @return instance of NetworkManager
	 */
	public NetworkManager(){
		if(log.isDebugEnabled()) log.debug("Creating network service registry");
		registry = new SimpleServiceRegistry(); 
	}
	
	/**
	 * Sets the ServiceConfig file
	 * @param serviceConfig
	 */
	public void setServiceConfig(Map serviceConfig){
		this.serviceConfig = serviceConfig;		
	}
	
	/**
	 * Start the process of binding to ports.  Review red5.properties file to see which port were binding to.
	 * @return void
	 */
	public void up(){
		if(networkStatus == STATUS_NETWORK_UP){
			log.warn("Network is already up, taking no action, call down() first or restart()");
			return;
		}
		
		// does application use secure socket layer
		if(USE_SSL) {
			try {
				addSSLSupport( registry );
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		log.info("Bringing up network services");
		try{
			// Loop over the serviceConfig
			Iterator it = serviceConfig.keySet().iterator();
			while(it.hasNext()){
				String serviceName = (String) it.next();
				Map conf = (Map) serviceConfig.get(serviceName);
				int port = Integer.parseInt((String) conf.get("port"));
				String host = (String) conf.get("host");
				Object handler = conf.get("handler");
				// TODO: add support for other transport types, socket should be default
				TransportType transportType = TransportType.SOCKET;
				InetSocketAddress address = new InetSocketAddress(host,port);
				Service service = new Service(serviceName, transportType,address);
				if(handler instanceof IoHandlerAdapter){
					IoHandlerAdapter ioHandlerAdapter = (IoHandlerAdapter) handler;
					if(log.isDebugEnabled())
						log.debug("Binding IO Handler Adapter for: "+service);
					registry.bind(service, ioHandlerAdapter);
				} else if(handler instanceof ProtocolProvider){
					ProtocolProvider protocolProvider = (ProtocolProvider) handler;
					if(log.isDebugEnabled())
						log.debug("Binding Protocol Provider for: "+service);
					registry.bind(service, protocolProvider);
				}
			}
			networkStatus = STATUS_NETWORK_UP;
			log.info("Network services up");
		} catch(Exception ex){
			log.error("Error bringing up network", ex);
			networkStatus = STATUS_NETWORK_ERROR;
		}
	}
	
	/**
	 * Brings the network down by unbinding ports.
	 * @return void
	 */
	public void down(){
		if(networkStatus != STATUS_NETWORK_UP){
			log.warn("Network is already down, taking no action, call up() first.");
			return;
		}
		log.info("Shutting down network services");
		try{
			registry.unbindAll();
			networkStatus = STATUS_NETWORK_DOWN;
			log.info("Network services down");
		} catch(Exception ex){
			log.error("Error shutting down network", ex);
			networkStatus = STATUS_NETWORK_ERROR;
		}
	}
	
	/**
	 * Restarts the Server by checking the status and then calling the proper method
	 * @return void
	 */
	public void restart(){
		if(networkStatus == STATUS_NETWORK_UP){
			down();
		}
		if(networkStatus != STATUS_NETWORK_UP){
			up();
		}
	}
	
	// Note: If so we should add a setter setSSLSupport(boolean enable);
	private static void addSSLSupport( ServiceRegistry registry )
	  throws Exception
	{
		/* disabled for now as it was needing the ssl classes to compile
		SSLFilter sslFilter =
	    new SSLFilter( BogusSSLContextFactory.getInstance( true ) );
	    IoAcceptor acceptor = registry.getIoAcceptor( TransportType.SOCKET );
	    acceptor.getFilterChain().addLast( "sslFilter", sslFilter );
	    System.out.println( "SSL ON" );
	    */
	}
	
	/*
	private static void addLogger( ServiceRegistry registry ){
		IoAcceptor acceptor = registry.getIoAcceptor( TransportType.SOCKET );
	    acceptor.getFilterChain().addLast( "logger", new IoLoggingFilter() );
	    System.out.println( "Logging ON" );
	}
	*/
	
	
}
