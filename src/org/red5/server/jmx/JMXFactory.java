package org.red5.server.jmx;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.apache.log4j.Logger;

/**
 * Provides access to the platform MBeanServer as well as registration, unregistration, and creation of new MBean instances. Creation and registration is performed using StandardMBean wrappers.
 *
 * References:
 * http://www.onjava.com/pub/a/onjava/2004/09/29/tigerjmx.html?page=1
 * http://java.sun.com/developer/JDCTechTips/2005/tt0315.html#2
 *
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class JMXFactory {

	private static Logger log = Logger.getLogger(JMXFactory.class);

	private static String domain = "org.red5.server";

	private static MBeanServer mbs;

	static {
		// grab a reference to the "platform" MBeanServer
		mbs = ManagementFactory.getPlatformMBeanServer();
	}

	public static ObjectName createMBean(String className, String attributes) {
		log.info("Create the " + className + " MBean within the MBeanServer");
		ObjectName objName = null;
		try {
			StringBuilder objectNameStr = new StringBuilder(domain);
			objectNameStr.append(":type=");
			objectNameStr.append(className
					.substring(className.lastIndexOf(".") + 1));
			objectNameStr.append(",");
			objectNameStr.append(attributes);
			log.info("ObjectName = " + objectNameStr);
			objName = new ObjectName(objectNameStr.toString());
			mbs.createMBean(className, objName);
		} catch (Exception e) {
			log.error("Could not create the " + className + " MBean", e);
		}
		return objName;
	}

	public static ObjectName createSimpleMBean(String className,
			String objectNameStr) {
		log.info("Create the " + className + " MBean within the MBeanServer");
		log.info("ObjectName = " + objectNameStr);
		try {
			ObjectName objectName = ObjectName.getInstance(objectNameStr);
			mbs.createMBean(className, objectName);
			return objectName;
		} catch (Exception e) {
			log.error("Could not create the " + className + " MBean", e);
		}
		return null;
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
					new ObjectName(domain + ":type=" + cName));
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
			mbs
					.registerMBean(new StandardMBean(instance, interfaceClass),
							new ObjectName(domain + ":type=" + cName + ",name="
									+ name));
			status = true;
		} catch (Exception e) {
			log.error("Could not register the " + className + " MBean", e);
		}
		return status;
	}

	public static boolean registerNewMBean(String className,
			Class interfaceClass) {
		boolean status = false;
		try {
			String cName = className;
			if (cName.indexOf('.') != -1) {
				cName = cName.substring(cName.lastIndexOf('.')).replaceFirst(
						"[\\.]", "");
			}
			log.debug("Register name: " + cName);
			mbs.registerMBean(new StandardMBean(Class.forName(className)
					.newInstance(), interfaceClass), new ObjectName(domain
					+ ":type=" + cName));
			status = true;
		} catch (Exception e) {
			log.error("Could not register the " + className + " MBean", e);
		}
		return status;
	}

	public static boolean registerNewMBean(String className,
			Class interfaceClass, String name) {
		boolean status = false;
		try {
			String cName = className;
			if (cName.indexOf('.') != -1) {
				cName = cName.substring(cName.lastIndexOf('.')).replaceFirst(
						"[\\.]", "");
			}
			log.debug("Register name: " + cName);
			mbs.registerMBean(new StandardMBean(Class.forName(className)
					.newInstance(), interfaceClass), new ObjectName(domain
					+ ":type=" + cName + ",name=" + name));
			status = true;
		} catch (Exception e) {
			log.error("Could not register the " + className + " MBean", e);
		}
		return status;
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
					mbs.unregisterMBean(oName);
					//set flag based on registration status
					unregistered = mbs.isRegistered(oName);
				} else {
					log.debug("Mbean is not currently registered");
				}
			} catch (Exception e) {
				log.error("Exception unregistering mbean", e);
			}
		}
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

	public static String getDefaultDomain() {
		return domain;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		JMXFactory.domain = domain;
	}

	public static MBeanServer getMBeanServer() {
		return mbs;
	}

}