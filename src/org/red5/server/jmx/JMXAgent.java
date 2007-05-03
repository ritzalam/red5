package org.red5.server.jmx;

import java.util.ArrayList;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.apache.log4j.Logger;

import com.sun.jdmk.comm.HtmlAdaptorServer;

/**
 * Provides the HTML adapter and registration of MBeans.
 *
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class JMXAgent {

	private static Logger log = Logger.getLogger(JMXAgent.class);

	private static String htmlAdapterPort = "8082";

	private static MBeanServer mbs;

	public void init() {
		// get the server
		if (null == mbs) {
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
		// setup the agent
		try {
			//instance an html adaptor
			int port = htmlAdapterPort == null ? 9080 : Integer
					.valueOf(htmlAdapterPort);
			HtmlAdaptorServer html = new HtmlAdaptorServer(port);
			ObjectName htmlName = new ObjectName(JMXFactory.getDefaultDomain()
					+ ":type=HtmlAdaptorServer,port=" + port);
			log.debug("Created HTML adaptor on port: " + port);
			//add the adaptor to the server
			mbs.registerMBean(html, htmlName);
			//start the adaptor
			html.start();

			log.debug("JMX default domain: " + mbs.getDefaultDomain());

		} catch (Exception e) {
			log.error("Error in setup of JMX subsystem", e);
		}
	}

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

	public String getHtmlAdapterPort() {
		return htmlAdapterPort;
	}

	public void setHtmlAdapterPort(String htmlAdapterPort) {
		JMXAgent.htmlAdapterPort = htmlAdapterPort;
	}

}