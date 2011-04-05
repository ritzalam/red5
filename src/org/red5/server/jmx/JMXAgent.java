package org.red5.server.jmx;

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

import java.io.File;
import java.io.IOException;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides the connection adapters as well as registration and
 * unregistration of MBeans.
 *
 * @author The Red5 Project (red5@osflash.org)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class JMXAgent implements NotificationListener {

	private static JMXConnectorServer cs;

	private static boolean enableRmiAdapter;

	private static boolean startRegistry;

	private static boolean enableSsl;

	private static boolean enableMinaMonitor;

	private static Logger log = LoggerFactory.getLogger(JMXAgent.class);

	private static MBeanServer mbs;

	private static String rmiAdapterPort = "9999";

	private static String rmiAdapterRemotePort = "";

	private static String rmiAdapterHost = "localhost";

	private static String remotePasswordProperties;

	private static String remoteAccessProperties;

	private static String remoteSSLKeystore;

	private static String remoteSSLKeystorePass;

	static {
		//in the war version the jmx factory is not created before
		//registration starts ?? so we check for it here and init
		//if needed
		if (null == mbs) {
			mbs = JMXFactory.getMBeanServer();
		}
	}

	/**
	 * Convenience to remove packages etc from a class name.
	 * 
	 * @param className class name to trim
	 * @return trimmed class name
	 */
	public static String trimClassName(String className) {
		if (StringUtils.isNotBlank(className)) {
			if (className.indexOf('.') != -1) {
				//strip package stuff
				className = className.substring(className.lastIndexOf('.') + 1);
			}
		}
		return className;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static boolean registerMBean(Object instance, String className, Class interfaceClass) {
		boolean status = false;
		try {
			String cName = className;
			if (cName.indexOf('.') != -1) {
				cName = cName.substring(cName.lastIndexOf('.')).replaceFirst("[\\.]", "");
			}
			log.debug("Register name: {}", cName);
			mbs.registerMBean(new StandardMBean(instance, interfaceClass), new ObjectName(JMXFactory.getDefaultDomain()
					+ ":type=" + cName));
			status = true;
		} catch (InstanceAlreadyExistsException iaee) {
			log.debug("Already registered: {}", className);
		} catch (Exception e) {
			log.error("Could not register the {} MBean. {}", className, e);
		}
		return status;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static boolean registerMBean(Object instance, String className, Class interfaceClass, ObjectName name) {
		boolean status = false;
		try {
			String cName = className;
			if (cName.indexOf('.') != -1) {
				cName = cName.substring(cName.lastIndexOf('.')).replaceFirst("[\\.]", "");
			}
			log.debug("Register name: {}", cName);
			mbs.registerMBean(new StandardMBean(instance, interfaceClass), name);
			status = true;
		} catch (InstanceAlreadyExistsException iaee) {
			log.debug("Already registered: {}", className);
		} catch (Exception e) {
			log.error("Could not register the {} MBean. {}", className, e);
		}
		return status;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static boolean registerMBean(Object instance, String className, Class interfaceClass, String name) {
		boolean status = false;
		try {
			String cName = className;
			if (cName.indexOf('.') != -1) {
				cName = cName.substring(cName.lastIndexOf('.')).replaceFirst("[\\.]", "");
			}
			log.debug("Register name: {}", cName);
			mbs.registerMBean(new StandardMBean(instance, interfaceClass), new ObjectName(JMXFactory.getDefaultDomain()
					+ ":type=" + cName + ",name=" + name));
			status = true;
		} catch (InstanceAlreadyExistsException iaee) {
			log.debug("Already registered: {}", className);
		} catch (Exception e) {
			log.error("Could not register the {} MBean. {}", className, e);
		}
		return status;
	}

	/**
	 * Shuts down any instanced connectors.
	 */
	@SuppressWarnings("cast")
	public static void shutdown() {
		log.info("Shutting down JMX agent");
		if (null != cs) {
			try {
				//stop the connector
				cs.stop();
			} catch (Exception e) {
				log.error("Exception stopping JMXConnector server {}", e);
			}
		}
		try {
			//unregister all the currently registered red5 mbeans
			String domain = JMXFactory.getDefaultDomain();
			for (ObjectName oname : mbs.queryNames(new ObjectName(domain + ":*"), null)) {
				log.debug("Bean domain: {}", oname.getDomain());
				if (domain.equals(oname.getDomain())) {
					unregisterMBean(oname);
				}
			}
		} catch (Exception e) {
			log.error("Exception unregistering mbeans {}", e);
		}

	}

	/**
	 * Unregisters an mbean instance. If the instance is not found or if a failure occurs, false will be returned.
	 * @param oName bean instance
	 * @return true if success; false if instance not found or failure
	 */
	public static boolean unregisterMBean(ObjectName oName) {
		boolean unregistered = false;
		if (null != oName) {
			try {
				if (mbs.isRegistered(oName)) {
					log.debug("Mbean is registered");
					mbs.unregisterMBean(oName);
					//set flag based on registration status
					unregistered = !mbs.isRegistered(oName);
				} else {
					log.debug("Mbean is not currently registered");
				}
			} catch (Exception e) {
				log.warn("Exception unregistering mbean {}", e);
			}
		}
		log.debug("leaving unregisterMBean...");
		return unregistered;
	}

	/**
	 * Updates a named attribute of a registered mbean.
	 *
	 * @param oName object name
	 * @param key key
	 * @param value new value
	 * @return true if success; false othwerwise
	 */
	public static boolean updateMBeanAttribute(ObjectName oName, String key, int value) {
		boolean updated = false;
		if (null != oName) {
			try {
				// update the attribute
				if (mbs.isRegistered(oName)) {
					mbs.setAttribute(oName, new javax.management.Attribute("key", value));
					updated = true;
				}
			} catch (Exception e) {
				log.error("Exception updating mbean attribute", e);
			}
		}
		return updated;
	}

	/**
	 * Updates a named attribute of a registered mbean.
	 *
	 * @param oName object name
	 * @param key key
	 * @param value new value
	 * @return true if success; false otherwise
	 */
	public static boolean updateMBeanAttribute(ObjectName oName, String key, String value) {
		boolean updated = false;
		if (null != oName) {
			try {
				// update the attribute
				if (mbs.isRegistered(oName)) {
					mbs.setAttribute(oName, new javax.management.Attribute("key", value));
					updated = true;
				}
			} catch (Exception e) {
				log.error("Exception updating mbean attribute", e);
			}
		}
		return updated;
	}

	public void handleNotification(Notification notification, Object handback) {
		log.debug("handleNotification {}", notification.getMessage());
	}

	public void init() {		
		//environmental var holder
		HashMap<String, Object> env = null;

		if (enableRmiAdapter) {
			// Create an RMI connector server
			log.debug("Create an RMI connector server");

			// bind the rmi hostname for systems with nat and multiple binded addresses !
			System.setProperty("java.rmi.server.hostname", rmiAdapterHost);

			try {

				Registry r = null;
				try {
					//lookup the registry
					r = LocateRegistry.getRegistry(Integer.valueOf(rmiAdapterPort));
					//ensure we are not already registered with the registry
					for (String regName : r.list()) {
						if (regName.equals("red5")) {
							//unbind connector from rmi registry
							r.unbind("red5");
						}
					}
				} catch (RemoteException re) {
					log.info("RMI Registry server was not found on port " + rmiAdapterPort);
					//if we didnt find the registry and the user wants it created
					if (startRegistry) {
						log.info("Starting an internal RMI registry");
						r = LocateRegistry.createRegistry(Integer.valueOf(rmiAdapterPort));
					}
				}

				JMXServiceURL url = null;

				// Configure the remote objects exported port for firewalls !!
				if (StringUtils.isNotEmpty(rmiAdapterRemotePort)) {
					url = new JMXServiceURL("service:jmx:rmi://" + rmiAdapterHost + ":" + rmiAdapterRemotePort
							+ "/jndi/rmi://" + rmiAdapterHost + ":" + rmiAdapterPort + "/red5");
				} else {
					url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://:" + rmiAdapterPort + "/red5");
				}

				log.info("JMXServiceUrl is: {}", url);

				//if SSL is requested to secure rmi connections
				if (enableSsl) {

					// Setup keystore for SSL transparently
					System.setProperty("javax.net.ssl.keyStore", remoteSSLKeystore);
					System.setProperty("javax.net.ssl.keyStorePassword", remoteSSLKeystorePass);

					// Environment map
					log.debug("Initialize the environment map");
					env = new HashMap<String, Object>();
					// Provide SSL-based RMI socket factories
					SslRMIClientSocketFactory csf = new SslRMIClientSocketFactory();
					SslRMIServerSocketFactory ssf = new SslRMIServerSocketFactory();
					env.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE, csf);
					env.put(RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE, ssf);
				}

				//if authentication is requested
				if (StringUtils.isNotBlank(remoteAccessProperties)) {
					//if ssl is not used this will be null
					if (null == env) {
						env = new HashMap<String, Object>();
					}
					//check the existance of the files
					//in the war version the full path is needed
					File file = new File(remoteAccessProperties);
					if (!file.exists() && remoteAccessProperties.indexOf(System.getProperty("red5.config_root")) != 0) {
						log.debug("Access file was not found on path, will prepend config_root");
						//pre-pend the system property set in war startup
						remoteAccessProperties = System.getProperty("red5.config_root") + '/' + remoteAccessProperties;
						remotePasswordProperties = System.getProperty("red5.config_root") + '/'
								+ remotePasswordProperties;
					}
					//strip "file:" prefixing
					env.put("jmx.remote.x.access.file", remoteAccessProperties.replace("file:", ""));
					env.put("jmx.remote.x.password.file", remotePasswordProperties.replace("file:", ""));
				}

				// create the connector server
				cs = JMXConnectorServerFactory.newJMXConnectorServer(url, env, mbs);
				// add a listener for shutdown
				cs.addNotificationListener(this, null, null);
				// Start the RMI connector server
				log.debug("Start the RMI connector server");
				cs.start();
				log.info("JMX RMI connector server successfully started");
			} catch (ConnectException e) {
				log.warn("Could not establish RMI connection to port " + rmiAdapterPort
						+ ", please make sure \"rmiregistry\" is running and configured to listen on this port.");
			} catch (IOException e) {
				String errMsg = e.getMessage();
				if (errMsg.indexOf("NameAlreadyBoundException") != -1) {
					log.error("JMX connector (red5) already registered, you will need to restart your rmiregistry");
				} else {
					log.error("{}", e);
				}
			} catch (Exception e) {
				log.error("Error in setup of JMX subsystem (RMI connector)", e);
			}
		} else {
			log.info("JMX RMI adapter was not enabled");
		}
	}

	public void setEnableRmiAdapter(boolean enableRmiAdapter) {
		JMXAgent.enableRmiAdapter = enableRmiAdapter;
	}

	public void setEnableSsl(boolean enableSsl) {
		JMXAgent.enableSsl = enableSsl;
	}

	public void setRemoteAccessProperties(String remoteAccessProperties) {
		JMXAgent.remoteAccessProperties = remoteAccessProperties;
	}

	public void setRemotePasswordProperties(String remotePasswordProperties) {
		JMXAgent.remotePasswordProperties = remotePasswordProperties;
	}

	public void setRemoteSSLKeystore(String remoteSSLKeystore) {
		JMXAgent.remoteSSLKeystore = remoteSSLKeystore;
	}

	public void setRemoteSSLKeystorePass(String remoteSSLKeystorePass) {
		JMXAgent.remoteSSLKeystorePass = remoteSSLKeystorePass;
	}

	public void setRmiAdapterRemotePort(String rmiAdapterRemotePort) {
		JMXAgent.rmiAdapterRemotePort = rmiAdapterRemotePort;
	}

	public void setRmiAdapterPort(String rmiAdapterPort) {
		JMXAgent.rmiAdapterPort = rmiAdapterPort;
	}

	public void setRmiAdapterHost(String rmiAdapterHost) {
		JMXAgent.rmiAdapterHost = rmiAdapterHost;
	}

	public void setStartRegistry(boolean startRegistry) {
		JMXAgent.startRegistry = startRegistry;
	}

	public void setEnableMinaMonitor(boolean enableMinaMonitor) {
		JMXAgent.enableMinaMonitor = enableMinaMonitor;
	}

	public static boolean isEnableMinaMonitor() {
		return enableMinaMonitor;
	}

}
