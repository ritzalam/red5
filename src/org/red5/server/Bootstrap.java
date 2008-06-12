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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

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
		//look over the libraries and remove the old versions
		scrubList(urls);
		// add config dir
		urls.add(new File(conf).toURI().toURL());
		//
		System.out.println(urls.size() + " items in the classpath");

		//loop thru all the current urls
		//System.out.println("Classpath: ");
		//for (URL url : urls) {
		//	System.out.println(url.toExternalForm());
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
	
	/**
	 * Removes older versions of libraries from a given list based
	 * on their version numbers.
	 * 
	 * @param list
	 */
	private final static void scrubList(List<URL> list) {
		Pattern punct = Pattern.compile("\\p{Punct}");
		Set<URL> removalList = new HashSet<URL>(list.size());
		String topName = null;
		String checkName = null;
		URL[] urls = list.toArray(new URL[0]);
		for (URL top : urls) {
			if (removalList.contains(top)) {
				continue;
			}
			topName = parseUrl(top);
			//by default we will get rid of testing libraries and jetty
			if (topName.startsWith("jetty") || topName.startsWith("grobo") || topName.startsWith("junit") || topName.startsWith("ivy")) {
				removalList.add(top);
				continue;
			}
			int topFirstDash = topName.indexOf('-');
			int topSecondDash = topName.indexOf('-', topFirstDash + 1);
			//if theres no dash then just grab the first 3 chars
			String prefix = topName.substring(0, topFirstDash != -1 ? topFirstDash : 3);
    		for (URL check : list) {
    			if (removalList.contains(check)) {
    				continue;
    			}    			
    			checkName = parseUrl(check);
    			//if its the same lib just continue with the next
    			if (checkName.equals(topName)) {
    				continue;
    			}
    			//if the last character is a dash then skip it
    			if (checkName.endsWith("-")) {
    				continue;
    			}
    			//check starts with to see if we should do version check
    			if (!checkName.startsWith(prefix)) {
    				continue;    				
    			}    			
    			//check for next dash
    			if (topSecondDash > 0) {
    				if (checkName.length() <= topSecondDash) {
    					continue;
    				}
        			//check for second dash in check lib at same position
    				if (checkName.charAt(topSecondDash) != '-') {
    					continue;
    				}
    				//split the names
    				String[] topSubs = topName.split("-");
    				String[] checkSubs = checkName.split("-");
    				//check lib type "spring-aop" vs "spring-orm"
    				if (!topSubs[1].equals(checkSubs[1])) {
    					continue;
    				}
    				//see if next entry is a number
    				if (!Character.isDigit(topSubs[2].charAt(0)) && !Character.isDigit(checkSubs[2].charAt(0))) {
    					//check next lib name section for a match
        				if (!topSubs[2].equals(checkSubs[2])) {
        					continue;
        				}
    				}
    			}
    			
    			//do the version check
    			
    			//read from end to get version info
    			String checkVers = checkName.substring(topSecondDash != -1 ? (topSecondDash + 1) : (topFirstDash + 1));
    			    			
    			if (checkVers.startsWith("-")) {
    				continue;
    			}
    			
    			//get top libs version info
    			String topVers = topName.substring(topSecondDash != -1 ? (topSecondDash + 1) : (topFirstDash + 1));
    			int topThirdDash = -1;
    			String topThirdName = null;
    			if (!Character.isDigit(topVers.charAt(0))) {
        			//check if third level lib name matches
    				topThirdDash = topVers.indexOf('-');
    				//no version most likely exists
    				if (topThirdDash == -1) {
    				    continue;
    				}
    				topThirdName = topVers.substring(0, topThirdDash);
    				topVers = topVers.substring(topThirdDash + 1);
    			}        			
    			
    			//if check version starts with a non-number skip it
    			int checkThirdDash = -1;
    			String checkThirdName = null;
    			if (!Character.isDigit(checkVers.charAt(0))) {
        			//check if third level lib name matches
    				checkThirdDash = checkVers.indexOf('-');
    				//no version most likely exists
    				if (checkThirdDash == -1) {
    				    continue;
    				}
    				checkThirdName = checkVers.substring(0, checkThirdDash);
    				if (topThirdName == null || !topThirdName.equals(checkThirdName)) {
    					continue;
    				}
    				checkVers = checkVers.substring(checkThirdDash + 1);
    				//if not
        			if (!Character.isDigit(checkVers.charAt(0))) {
        				continue;
        			}
    			}	
    			
    			if (topThirdName != null && checkThirdName == null) {
    				continue;
    			}
    			
    			//check major
    			String[] topVersion = punct.split(topVers);
    			String[] checkVersion = punct.split(checkVers);
    			
    			int topVersionNumber = Integer.valueOf(topVersion[0] + topVersion[1] + (topVersion.length > 2 ? topVersion[2] : '0')).intValue();
    			int checkVersionNumber = Integer.valueOf(checkVersion[0] + checkVersion[1] + (checkVersion.length > 2 ? checkVersion[2] : '0')).intValue();
    			
    			if (topVersionNumber >= checkVersionNumber) {
    				//remove it
    				removalList.add(check);
    			} else {
    				removalList.add(top);
    				break;
    			}

    		}
		}
		//remove the old libs
		list.removeAll(removalList);
	}
	
	private static String parseUrl(URL url) {
		String external = url.toExternalForm().toLowerCase();
		//get everything after the last slash
		String[] parts = external.split("/");
		//last part
		String libName = parts[parts.length - 1];
		//strip .jar
		libName = libName.substring(0, libName.length() - 4);
		return libName;
	}
	
}