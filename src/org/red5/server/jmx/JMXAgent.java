package org.red5.server.jmx;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 *
 * Copyright (c) 2006-2007 by respective authors (see below). All rights reserved.
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

import java.io.IOException;
import java.rmi.ConnectException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.log4j.Logger;

import com.sun.jdmk.comm.HtmlAdaptorServer;

/**
 * Provides the connection adapters as well as registration and
 * unregistration of MBeans.
 *
 * @author The Red5 Project (red5@osflash.org)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class JMXAgent implements NotificationListener {

	private static JMXConnectorServer cs;

	private static boolean enableHtmlAdapter;

	private static boolean enableRmiAdapter;

	private static HtmlAdaptorServer html;

	private static String htmlAdapterPort = "8082";

	private static Logger log = Logger.getLogger(JMXAgent.class);

	private static MBeanServer mbs;

	private static String rmiAdapterPort = "9999";

	public static boolean registerMBean(Object instance, String className,
			Class interfaceClass) {
		boolean status = false;
		try {
			String cName = className;
			if (cName.indexOf('.') != -1) {
				cName = cName.substring(cName.lastIndexOf('.')).replaceFirst(
						"[\\.]", "");
			}
			log.debug("Register name: " + cName);
			mbs.registerMBean(new StandardMBean(instance, interfaceClass),
					new ObjectName(JMXFactory.getDefaultDomain() + ":type="
							+ cName));
			status = true;
		} catch (Exception e) {
			log.error("Could not register the " + className + " MBean", e);
		}
		return status;
	}

	public static boolean registerMBean(Object instance, String className,
			Class interfaceClass, String name) {
		boolean status = false;
		try {
			String cName = className;
			if (cName.indexOf('.') != -1) {
				cName = cName.substring(cName.lastIndexOf('.')).replaceFirst(
						"[\\.]", "");
			}
			log.debug("Register name: " + cName);
			mbs.registerMBean(new StandardMBean(instance, interfaceClass),
					new ObjectName(JMXFactory.getDefaultDomain() + ":type="
							+ cName + ",name=" + name));
			status = true;
		} catch (Exception e) {
			log.error("Could not register the " + className + " MBean", e);
		}
		return status;
	}

	/**
	 * Shuts down any instanced connectors.
	 */
	public static void shutdown() {
		log.info("Shutting down JMX agent");
		if (null != cs) {
			try {
				//stop the connector
				cs.stop();
			} catch (Exception e) {
				log.error("Exception stopping JMXConnector server", e);
			}
		}
		if (null != html) {
			html.stop();
		}
	}

	/**
	 * Unregisters an mbean instance. If the instance is not found or if a failure occurs, false will be returned.
	 * @param oName
	 * @return
	 */
	public static boolean unregisterMBean(ObjectName oName) {
		boolean unregistered = false;
		if (null != oName) {
			try {
				if (mbs.isRegistered(oName)) {
					log.debug("Mbean is registered");
					mbs.unregisterMBean(oName);
					//set flag based on registration status
					unregistered = mbs.isRegistered(oName);
				} else {
					log.debug("Mbean is not currently registered");
				}
			} catch (Exception e) {
				log.warn("Exception unregistering mbean", e);
			}
		}
		log.debug("leaving unregisterMBean...");
		return unregistered;
	}

	/**
	 * Updates a named attribute of a registered mbean.
	 *
	 * @param oName
	 * @param key
	 * @param value
	 * @return
	 */
	public static boolean updateMBeanAttribute(ObjectName oName, String key,
			int value) {
		boolean updated = false;
		if (null != oName) {
			try {
				// update the attribute
				if (mbs.isRegistered(oName)) {
					mbs.setAttribute(oName, new javax.management.Attribute(
							"key", value));
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
	 * @param oName
	 * @param key
	 * @param value
	 * @return
	 */
	public static boolean updateMBeanAttribute(ObjectName oName, String key,
			String value) {
		boolean updated = false;
		if (null != oName) {
			try {
				// update the attribute
				if (mbs.isRegistered(oName)) {
					mbs.setAttribute(oName, new javax.management.Attribute(
							"key", value));
					updated = true;
				}
			} catch (Exception e) {
				log.error("Exception updating mbean attribute", e);
			}
		}
		return updated;
	}

	public String getHtmlAdapterPort() {
		return htmlAdapterPort;
	}

	public void handleNotification(Notification notification, Object handback) {
		log.debug("handleNotification " + notification.getMessage());
	}

	public void init() {
		// get the server
		if (null == mbs) {
			log.debug("MBeanServer was null");
			// lookup the MBeanServer for our domain
			ArrayList<MBeanServer> serverList = MBeanServerFactory
					.findMBeanServer(null);
			for (MBeanServer svr : serverList) {
				log.debug("Default domain: " + svr.getDefaultDomain());
				//accept the first one
				if (null == mbs) {
					mbs = svr;
				}
			}
		}
		if (enableHtmlAdapter) {
			// setup the adapter
			try {
				//instance an html adaptor
				int port = htmlAdapterPort == null ? 8082 : Integer
						.valueOf(htmlAdapterPort);
				html = new HtmlAdaptorServer(port);
				ObjectName htmlName = new ObjectName(JMXFactory
						.getDefaultDomain()
						+ ":type=HtmlAdaptorServer,port=" + port);
				log.debug("Created HTML adaptor on port: " + port);
				//add the adaptor to the server
				mbs.registerMBean(html, htmlName);
				//start the adaptor
				html.start();
				log.info("JMX HTML connector server successfully started");

			} catch (Exception e) {
				log.error("Error in setup of JMX subsystem (HTML adapter)", e);
			}
		} else {
			log.info("JMX HTML adapter was not enabled");
		}
		if (enableRmiAdapter) {
			// Create an RMI connector server
			log.debug("Create an RMI connector server");
			try {
				//ensure we are not already registered with the registry
				Registry r = LocateRegistry.getRegistry(Integer
						.valueOf(rmiAdapterPort));
				for (String regName : r.list()) {
					if (regName.equals("red5")) {
						//unbind connector from rmi registry
						r.unbind("red5");
					}
				}
				JMXServiceURL url = new JMXServiceURL(
						"service:jmx:rmi:///jndi/rmi://:" + rmiAdapterPort
								+ "/red5");
				cs = JMXConnectorServerFactory.newJMXConnectorServer(url, null,
						mbs);
				// add a listener for shutdown
				cs.addNotificationListener(this, null, null);
				// Start the RMI connector server
				log.debug("Start the RMI connector server");
				cs.start();
				log.info("JMX RMI connector server successfully started");
			} catch (ConnectException e) {
				log
						.warn("Could not establish RMI connection to port "
								+ rmiAdapterPort
								+ ", please make sure \"rmiregistry\" is running and configured to listen on this port.");
			} catch (IOException e) {
				String errMsg = e.getMessage();
				if (errMsg.indexOf("NameAlreadyBoundException") != -1) {
					log
							.error("JMX connector (red5) already registered, you will need to restart your rmiregistry");
				} else {
					log.error(e);
				}
			} catch (Exception e) {
				log.error("Error in setup of JMX subsystem (RMI connector)", e);
			}
		} else {
			log.info("JMX RMI adapter was not enabled");
		}
	}

	public void setEnableHtmlAdapter(boolean enableHtmlAdapter) {
		JMXAgent.enableHtmlAdapter = enableHtmlAdapter;
	}

	public void setEnableHtmlAdapter(String enableHtmlAdapterString) {
		JMXAgent.enableHtmlAdapter = enableHtmlAdapterString
				.matches("true|on|yes");
	}

	public void setEnableRmiAdapter(boolean enableRmiAdapter) {
		JMXAgent.enableRmiAdapter = enableRmiAdapter;
	}

	public void setEnableRmiAdapter(String enableRmiAdapterString) {
		JMXAgent.enableRmiAdapter = enableRmiAdapterString
				.matches("true|on|yes");
	}

	public void setHtmlAdapterPort(String htmlAdapterPort) {
		JMXAgent.htmlAdapterPort = htmlAdapterPort;
	}

	public void setRmiAdapterPort(String rmiAdapterPort) {
		JMXAgent.rmiAdapterPort = rmiAdapterPort;
	}

}