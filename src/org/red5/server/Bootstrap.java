package org.red5.server;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2008 by respective authors (see below). All rights reserved.
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
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.red5.server.api.Red5;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.FileSystemXmlApplicationContext;

/**
 * Boot-straps Red5 using the latest available jars found in <i>red5.home/lib</i> directory.
 *
 * @author The Red5 Project (red5@osflash.org)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class Bootstrap {

	public static void launch(URLClassLoader loader) {
		System.setProperty("red5.deployment.type", "bootstrap");
		try {				
			//set default for loading classes with url loader
			loader.setDefaultAssertionStatus(false);
			//create a logger before anything else happens
			Logger log = LoggerFactory.getLogger(Bootstrap.class);
			log.info("{} (http://www.osflash.org/red5)", Red5.getVersion());
			//create red5 app context
			FileSystemXmlApplicationContext ctx = new FileSystemXmlApplicationContext(new String[]{
					"classpath:/red5.xml"}, false);
			ctx.setClassLoader(loader);
			ctx.refresh();
			ctx.getBeanFactory().setBeanClassLoader(loader);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws Exception {

		// look for red5 root first as a system property
		String root = System.getProperty("red5.root");

		// if root is null find out current directory and use it as root
		if (root == null) {
			File here = new File("thisisadummyfile");
			if (!here.createNewFile()) {
				System.err.println("Could not determine current directory");
				System.exit(1);
			} else {
				root = here.getCanonicalPath().replaceFirst("thisisadummyfile",
						"");
				System.out.println("Current directory: " + root);
				if (!here.delete()) {
					here.deleteOnExit();
				}
				here = null;
				//flip slashes
				root = root.replaceAll("\\\\", "/");
				//drop last slash if exists
				if (root.charAt(root.length()-1) == '/') {
					root = root.substring(0, root.length() - 1);
				}
				//set property
				System.setProperty("red5.root", root);
			}
		}
		
		System.out.println("Red5 root: " + root);
		
		// look for config dir
		String conf = System.getProperty("red5.config_root");

		// if root is not null and conf is null then default it
		if (root != null && conf == null) {
			conf = root + "/conf";
		}

		//flip slashes
		conf = conf.replaceAll("\\\\", "/");
		
		//set conf sysprop
		System.setProperty("red5.config_root", conf);

		System.out.println("Configuation root: " + conf);
		
		// expect a conf/red5.xml or we fail!
		File configFile = new File(conf, "red5.xml");
		if (configFile.exists() && configFile.canRead()) {
			System.setProperty("red5.conf_file", "red5.xml");
		} else {
			//fail
			System.err
					.println("Configuration file was not found, server cannot start. Location: "
							+ configFile.getCanonicalPath());
			System.exit(2);
		}

		// add the classes dir and each jar in lib to a List of URLs.
		List<URL> urls = new ArrayList<URL>(57); //use prime
		// add red5.jar
		urls.add(new File(root, "red5.jar").toURI().toURL());
		// add all other libs
		for (File lib : new File(root, "lib").listFiles()) {
			URL url = lib.toURI().toURL();
			urls.add(url);
		}
		// add config dir
		urls.add(new File(conf).toURI().toURL());
		//
		System.out.println(urls.size() + " items in the classpath");

		//loop thru all the current urls
		//for (URL url : urls) {
		//	System.out.println("Classpath entry: " + url.toExternalForm());
		//}

		ClassLoader parent = ClassLoader.getSystemClassLoader().getParent();
		
		// pass urls to a URLClassLoader
		URLClassLoader loader = new URLClassLoader(urls.toArray(new URL[0]), parent);
				
		// set the classloader to the current thread
		Thread.currentThread().setContextClassLoader(loader);
		
		// create a new instance of this class using new classloader
		Object boot = loader.loadClass("org.red5.server.Bootstrap").newInstance();
	
		Method m1 = boot.getClass().getMethod("launch", new Class[]{ URLClassLoader.class });
		m1.invoke(null, loader);
			
		System.out.println("Bootstrap complete");
	}
	

	
}