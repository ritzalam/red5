package org.red5.server.util;

/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright (c) 2006-2011 by respective authors (see below). All rights reserved.
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
 * Simple J2EE server detector, based on an idea created by Brian Wing Shun
 * Chan.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class ServerDetector {

	private static String type;

	private static boolean geronimo;

	private static boolean glassfish;

	private static boolean jboss;

	private static boolean jetty;

	private static boolean jonas;

	private static boolean resin;

	private static boolean tomcat;

	private static boolean weblogic;

	private static boolean websphere;

	public static final String GERONIMO_CLASS = "/org/apache/geronimo/system/main/Daemon.class";

	public static final String GERONIMO_ID = "geronimo";

	public static final String GLASSFISH_ID = "glassfish";

	public static final String GLASSFISH_SYSTEM_PROPERTY = "com.sun.aas.instanceRoot";

	public static final String JBOSS_CLASS = "/org/jboss/Main.class";

	public static final String JBOSS_ID = "jboss";

	public static final String JETTY_CLASS = "/org/mortbay/jetty/Server.class";

	public static final String JETTY_ID = "jetty";

	public static final String JONAS_CLASS = "/org/objectweb/jonas/server/Server.class";

	public static final String JONAS_ID = "jonas";

	public static final String RESIN_CLASS = "/com/caucho/server/resin/Resin.class";

	public static final String RESIN_ID = "resin";

	public static final String TOMCAT_BOOTSTRAP_CLASS = "/org/apache/catalina/startup/Bootstrap.class";

	public static final String TOMCAT_EMBEDDED_CLASS = "/org/apache/catalina/startup/Embedded.class";

	public static final String TOMCAT_ID = "tomcat";

	public static final String WEBLOGIC_CLASS = "/weblogic/Server.class";

	public static final String WEBLOGIC_ID = "weblogic";

	public static final String WEBSPHERE_CLASS = "/com/ibm/websphere/product/VersionInfo.class";

	public static final String WEBSPHERE_ID = "websphere";

	static {
		// do the check only once per classloader / execution
		type = getServerType();
	}

	private static String getServerType() {
		if (type == null) {
			
			String tmp = null;
			
			if (isGeronimo()) {
				tmp = GERONIMO_ID;
			} else if (isGlassfish()) {
				tmp = GLASSFISH_ID;
			} else if (isJBoss()) {
				tmp = JBOSS_ID;
			} else if (isJOnAS()) {
				tmp = JONAS_ID;
			} else if (isResin()) {
				tmp = RESIN_ID;
			} else if (isWebLogic()) {
				tmp = WEBLOGIC_ID;
			} else if (isWebSphere()) {
				tmp = WEBSPHERE_ID;
			}
			//check for tomcat or jetty - standalone or embedded
			if (isTomcat()) {
				if (tmp == null) {
					tmp = TOMCAT_ID;
				} else {
					tmp += "-" + TOMCAT_ID;
				}
			} else if (isJetty()) {
				if (tmp == null) {
					tmp = JETTY_ID;
				} else {
					tmp += "-" + JETTY_ID;
				}
			}

			if (tmp == null) {
				throw new RuntimeException("Server is not supported");
			}
			
			return tmp;
		} else {
			return type;
		}
		
	}

	public static boolean isGeronimo() {
		if (!geronimo) {
			geronimo = detect(GERONIMO_CLASS);
		}
		return geronimo;
	}

	public static boolean isGlassfish() {
		if (!glassfish) {
			String value = System.getProperty(GLASSFISH_SYSTEM_PROPERTY);
			if (value != null) {
				glassfish = true;
			}
		}
		return glassfish;
	}

	public static boolean isJBoss() {
		if (!jboss) {
			jboss = detect(JBOSS_CLASS);
		}
		return jboss;
	}

	public static boolean isJetty() {
		if (!jetty) {
			jetty = detect(JETTY_CLASS);
		}
		return jetty;
	}

	public static boolean isJOnAS() {
		if (!jonas) {
			jonas = detect(JONAS_CLASS);
		}
		return jonas;
	}

	public static boolean isResin() {
		if (!resin) {
			resin = detect(RESIN_CLASS);
		}
		return resin;
	}

	public static boolean isTomcat() {
		if (!tomcat) {
			tomcat = detect(TOMCAT_BOOTSTRAP_CLASS);
			//check embedded
			if (!tomcat) {
				tomcat = detect(TOMCAT_EMBEDDED_CLASS);
			}
		}
		return tomcat;
	}

	public static boolean isWebLogic() {
		if (!weblogic) {
			weblogic = detect(WEBLOGIC_CLASS);
		}
		return weblogic;
	}

	public static boolean isWebSphere() {
		if (!websphere) {
			websphere = detect(WEBSPHERE_CLASS);
		}
		return websphere;
	}

	private static boolean detect(String className) {
		try {
			ClassLoader.getSystemClassLoader().loadClass(className);
			return true;
		} catch (ClassNotFoundException cnfe) {
			try {
				//try the current classloader
				Thread.currentThread().getContextClassLoader().loadClass(className);
				return true;
			} catch (ClassNotFoundException cnfe2) {
				return false;
			}
		}
	}

}