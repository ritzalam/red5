package org.red5.server;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.red5.server.jmx.mxbeans.ShutdownMXBean;

/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 *
 * Copyright (c) 2006-2010 by respective authors (see below). All rights reserved.
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
 */

/**
 * Provides a means to cleanly shutdown an instance from the command line.
 *
 * @author The Red5 Project (red5@osflash.org)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class Shutdown {

	/**
	 * Connects to the given RMI port (default: 9999) and invokes shutdown on
	 * the loader.
	 *
	 * @param args The first parameter should be a port number
	 */
	@SuppressWarnings("cast")
	public static void main(String[] args) {
		try {

			String policyFile = System.getProperty("java.security.policy");
			if (policyFile == null) {
				System.setProperty("java.security.debug", "failure");
				System.setProperty("java.security.policy", "conf/red5.policy");
			}

			/*
			try {
			    // Enable the security manager
			    SecurityManager sm = new SecurityManager();
			    System.setSecurityManager(sm);
			} catch (SecurityException se) {
				System.err.println("Security manager already set");
			}
			*/

			JMXServiceURL url = null;
			JMXConnector jmxc = null;
			HashMap<String, Object> env = null;

			if (null == args || args.length < 1) {
				System.out.println("Attempting to connect to RMI port: 9999");
				url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://:9999/red5");
			} else {
				System.out.println("Attempting to connect to RMI port: " + args[0]);
				url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://:" + args[0] + "/red5");

				if (args.length > 1) {
					env = new HashMap<String, Object>(1);
					String[] credentials = new String[] { args[1], args[2] };
					env.put("jmx.remote.credentials", credentials);
				}
			}

			jmxc = JMXConnectorFactory.connect(url, env);
			MBeanServerConnection mbs = jmxc.getMBeanServerConnection();
			
			//class supporting shutdown
			ShutdownMXBean proxy = null;

			//check for loader registration
			ObjectName tomcatObjectName = new ObjectName("org.red5.server:type=TomcatLoader");
			ObjectName jettyObjectName = new ObjectName("org.red5.server:type=JettyLoader");
			ObjectName contextLoaderObjectName = new ObjectName("org.red5.server:type=ContextLoader");
			if (mbs.isRegistered(jettyObjectName)) {
				System.out.println("Red5 Jetty loader was found");
				proxy = JMX.newMXBeanProxy(mbs, jettyObjectName, ShutdownMXBean.class, true);
			} else if (mbs.isRegistered(tomcatObjectName)) {
				System.out.println("Red5 Tomcat loader was found");
				proxy = JMX.newMXBeanProxy(mbs, tomcatObjectName, ShutdownMXBean.class, true);
			} else if (mbs.isRegistered(contextLoaderObjectName)) {
				System.out.println("Red5 Context loader was found");	
				proxy = JMX.newMXBeanProxy(mbs, contextLoaderObjectName, ShutdownMXBean.class, true);
			} else {
				System.out.println("Red5 Loader was not found, is the server running?");
			}
			if (proxy != null) {
				System.out.println("Calling shutdown");
				proxy.shutdown();
			}
			jmxc.close();
		} catch (UndeclaredThrowableException e) {
			//ignore
		} catch (NullPointerException e) {
			//ignore
		} catch (Exception e) {
			log.warn("Exception {}", e);
		}

	}

}
