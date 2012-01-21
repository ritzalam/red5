/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2012 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.server.jmx;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides access to the MBeanServer as well as registration
 * and creation of new MBean instances. For most classes the creation
 * and registration is performed using StandardMBean wrappers.
 * <br />
 * References:
 * http://www.onjava.com/pub/a/onjava/2004/09/29/tigerjmx.html?page=1
 * http://java.sun.com/developer/JDCTechTips/2005/tt0315.html#2
 * <br />
 * Examples:
 * http://java.sun.com/javase/6/docs/technotes/guides/jmx/examples.html
 * <br />
 *
 * @author The Red5 Project (red5@osflash.org)
 * @author Paul Gregoire (mondain@gmail.com)
 */
@SuppressWarnings("cast")
public class JMXFactory {

	private static String domain = "org.red5.server";

	private static Logger log = LoggerFactory.getLogger(JMXFactory.class);

	private static MBeanServer mbs;

	static {
		// try the first mbean server before grabbing platform, this should
		// make things easier when using jboss or tomcats built in jmx.
		try {
			mbs = MBeanServerFactory.findMBeanServer(null).get(0);
		} catch (Exception e) {
			// grab a reference to the "platform" MBeanServer
			mbs = ManagementFactory.getPlatformMBeanServer();
		}
	}

	public static ObjectName createMBean(String className, String attributes) {
		log.info("Create the {} MBean within the MBeanServer", className);
		ObjectName objectName = null;
		try {
			StringBuilder objectNameStr = new StringBuilder(domain);
			objectNameStr.append(":type=");
			objectNameStr.append(className.substring(className.lastIndexOf(".") + 1));
			objectNameStr.append(',');
			objectNameStr.append(attributes);
			log.info("ObjectName = {}", objectNameStr);
			objectName = new ObjectName(objectNameStr.toString());
			if (!mbs.isRegistered(objectName)) {
				mbs.createMBean(className, objectName);
			} else {
				log.debug("MBean has already been created: {}", objectName);
			}
		} catch (Exception e) {
			log.error("Could not create the {} MBean. {}", className, e);
		}
		return objectName;
	}

	public static ObjectName createObjectName(String... strings) {
		ObjectName objName = null;
		StringBuilder sb = new StringBuilder(domain);
		sb.append(':');
		for (int i = 0, j = 1; i < strings.length; i += 2, j += 2) {
			//log.debug("------------" + strings[i] + " " + strings[j]);
			//sb.append(ObjectName.quote(strings[i]));
			sb.append(strings[i]);
			sb.append('=');
			//sb.append(ObjectName.quote(strings[j]));
			sb.append(strings[j]);
			sb.append(',');
		}
		sb.deleteCharAt(sb.length() - 1);
		try {
			log.info("Object name: {}", sb.toString());
			objName = new ObjectName(sb.toString());
		} catch (Exception e) {
			log.warn("Exception creating object name", e);
		}
		return objName;
	}

	public static ObjectName createSimpleMBean(String className, String objectNameStr) {
		log.info("Create the {} MBean within the MBeanServer", className);
		log.info("ObjectName = {}", objectNameStr);
		try {
			ObjectName objectName = ObjectName.getInstance(objectNameStr);
			if (!mbs.isRegistered(objectName)) {
				mbs.createMBean(className, objectName);
			} else {
				log.debug("MBean has already been created: {}", objectName);
			}
			return objectName;
		} catch (Exception e) {
			log.error("Could not create the {} MBean. {}", className, e);
		}
		return null;
	}

	public static String getDefaultDomain() {
		return domain;
	}

	public static MBeanServer getMBeanServer() {
		return mbs;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static boolean registerNewMBean(String className, Class interfaceClass) {
		boolean status = false;
		try {
			String cName = className;
			if (cName.indexOf('.') != -1) {
				cName = cName.substring(cName.lastIndexOf('.')).replaceFirst("[\\.]", "");
			}
			log.debug("Register name: " + cName);
			mbs.registerMBean(new StandardMBean(Class.forName(className).newInstance(), interfaceClass),
					new ObjectName(domain + ":type=" + cName));
			status = true;
		} catch (Exception e) {
			log.error("Could not register the " + className + " MBean", e);
		}
		return status;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static boolean registerNewMBean(String className, Class interfaceClass, ObjectName name) {
		boolean status = false;
		try {
			String cName = className;
			if (cName.indexOf('.') != -1) {
				cName = cName.substring(cName.lastIndexOf('.')).replaceFirst("[\\.]", "");
			}
			log.debug("Register name: " + cName);
			mbs.registerMBean(new StandardMBean(Class.forName(className).newInstance(), interfaceClass), name);
			status = true;
		} catch (Exception e) {
			log.error("Could not register the " + className + " MBean", e);
		}
		return status;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static boolean registerNewMBean(String className, Class interfaceClass, String name) {
		boolean status = false;
		try {
			String cName = className;
			if (cName.indexOf('.') != -1) {
				cName = cName.substring(cName.lastIndexOf('.')).replaceFirst("[\\.]", "");
			}
			log.debug("Register name: " + cName);
			mbs.registerMBean(new StandardMBean(Class.forName(className).newInstance(), interfaceClass),
					new ObjectName(domain + ":type=" + cName + ",name=" + name));
			status = true;
		} catch (Exception e) {
			log.error("Could not register the " + className + " MBean", e);
		}
		return status;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		JMXFactory.domain = domain;
	}

}