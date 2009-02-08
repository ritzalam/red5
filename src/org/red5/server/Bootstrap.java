package org.red5.server;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2009 by respective authors (see below). All rights reserved.
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.red5.classloading.ClassLoaderBuilder;

/**
 * Boot-straps Red5 using the latest available jars found in <i>red5.home/lib</i> directory.
 *
 * @author The Red5 Project (red5@osflash.org)
 * @author Paul Gregoire (mondain@gmail.com)
 * @author Dominick Accattato (daccattato@gmail.com)
 */
public class Bootstrap {
	
	/**
	 * BootStrapping entry point
	 * 
	 * @param args command line arguments
	 * @throws Exception if error occurs
	 */
	public static void main(String[] args) throws Exception {
		//retrieve path elements from system properties
		String root = getRed5Root();		
		getConfigurationRoot(root);
		
		//bootstrap dependencies and start red5
		bootStrap();
			
		System.out.println("Bootstrap complete");
	}

	/**
	 * Loads classloader with dependencies
	 * 
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 */
	private static void bootStrap()
			throws InstantiationException, IllegalAccessException,
			ClassNotFoundException, NoSuchMethodException,
			InvocationTargetException {
		
		// print the classpath
		//String classPath = System.getProperty("java.class.path");
		//System.out.printf("JVM classpath: %s\n", classPath);		
		
		System.setProperty("red5.deployment.type", "bootstrap");
		
		System.setProperty("sun.lang.ClassLoader.allowArraySyntax", "true");
		
		//check system property before forcing out selector
		if (System.getProperty("logback.ContextSelector") == null) {
			//set to use our logger
			System.setProperty("logback.ContextSelector", "org.red5.logging.LoggingContextSelector");
		}
		
		String policyFile = System.getProperty("java.security.policy");
		if (policyFile == null) {
			System.setProperty("java.security.debug", "all");			
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
		
		// build a ClassLoader
		ClassLoader loader = ClassLoaderBuilder.build();
		
		// create a new instance of this class using new classloader
		Object boot = Class.forName("org.red5.server.Launcher", false, loader).newInstance();
	
		Method m1 = boot.getClass().getMethod("launch", (Class[]) null);
		m1.invoke(boot, (Object[]) null);
	}

	/**
	 * Gets the configuration root
	 * 
	 * @param root
	 * @return
	 */
	private static String getConfigurationRoot(String root) {
		// look for config dir
		String conf = System.getProperty("red5.config_root");

		// if root is not null and conf is null then default it
		if (root != null && conf == null) {
			conf = root + "/conf";
		}

		//flip slashes only if windows based os
		if (File.separatorChar != '/') {
			conf = conf.replaceAll("\\\\", "/");
		}
		
		//set conf sysprop
		System.setProperty("red5.config_root", conf);
		System.out.printf("Configuation root: %s\n", conf);
		return conf;
	}

	/**
	 * Gets the Red5 root
	 * 
	 * @return
	 * @throws IOException
	 */
	private static String getRed5Root() throws IOException {
		// look for red5 root first as a system property
		String root = System.getProperty("red5.root");
		// if root is null check environmental
		if (root == null) {
			//check for env variable
			root = System.getenv("RED5_HOME");
		}		
		// if root is null find out current directory and use it as root
		if (root == null || ".".equals(root)) {
			root = System.getProperty("user.dir");
			//System.out.printf("Current directory: %s\n", root);
		}
		//if were on a windows based os flip the slashes
		if (File.separatorChar != '/') {
			root = root.replaceAll("\\\\", "/");
		}
		//drop last slash if exists
		if (root.charAt(root.length()-1) == '/') {
			root = root.substring(0, root.length() - 1);
		}	
		//set/reset property
		System.setProperty("red5.root", root);
		
		System.out.printf("Red5 root: %s\n", root);
		return root;
	}
	
}
