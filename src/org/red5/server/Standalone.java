package org.red5.server;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006 by respective authors (see below). All rights reserved.
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
import java.io.FileInputStream;
import java.util.Iterator;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.springframework.context.access.ContextSingletonBeanFactoryLocator;

/**
 * Entry point from which the server config file is loaded
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Dominick Accattato (Dominick@gmail.com)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */
public class Standalone {
	
	// Initialize Logging
	protected static Log log =
        LogFactory.getLog(Standalone.class.getName());
	
	protected static String red5Config = "red5.xml";

	public static DebugPooledByteBufferAllocator allocator = null;
	
	public static void raiseOriginalException(Throwable e) throws Throwable {
		// Search for root exception
		while (e.getCause() != null)
			e = e.getCause();
		
		throw e;
	}
	
	/**
	 * Main entry point for the Red5 Server usage java Standalone
	 * 
	 * @param args
	 *            String passed in that points to a red5.xml config file
	 */
	public static void main(String[] args) throws Exception, Throwable {
		
		if (false) {
			allocator = new DebugPooledByteBufferAllocator(true);
			ByteBuffer.setAllocator(allocator);
		}
		
		if(args.length == 1) {
			red5Config = args[0];
		} 
		
		long time = System.currentTimeMillis();
		
		log.info("RED5 Server (http://www.osflash.org/red5)");
		log.info("Loading red5 global context from: "+red5Config);

		// Detect root of Red5 configuration and set as system property
		String root;
		String classpath = System.getProperty("java.class.path");
		File fp = new File(red5Config);
		fp = fp.getCanonicalFile();
		if (!fp.isFile()) {
			// Given file does not exist, search it on the classpath
			String[] paths = classpath.split(System
					.getProperty("path.separator"));
			for (int i=0; i<paths.length; i++) {
				fp = new File(paths[i] + "/" + red5Config);
				fp = fp.getCanonicalFile();
				if (fp.isFile())
					break;
			}
		}
		
		if (!fp.isFile())
			throw new Exception("could not find configuration file "
					+ red5Config + " on your classpath " + classpath);
		
		root = fp.getAbsolutePath();
		root = root.replace('\\', '/');
		int idx = root.lastIndexOf('/');
		root = root.substring(0, idx);
		System.setProperty("red5.config_root", root);
		log.info("Setting configuation root to " + root);
		
		// Setup system properties so they can be evaluated by Jetty
		Properties props = new Properties();
		props.load(new FileInputStream(root + "/red5.properties"));
		Iterator it = props.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			if (key != null && !key.equals(""))
				System.setProperty(key, props.getProperty(key));
		}
		
		// Store root directory of Red5
		idx = root.lastIndexOf('/');
		root = root.substring(0, idx);
		if (System.getProperty("file.separator").equals("/"))
			// Workaround for linux systems
			root = "/" + root;
		System.setProperty("red5.root", root);
		log.info("Setting Red5 root to " + root);
		
		try {
			ContextSingletonBeanFactoryLocator.getInstance(red5Config)
					.useBeanFactory("red5.common");
		} catch (Exception e) {
			// Don't raise wrapped exceptions as their stacktraces may confuse
			// people...
			raiseOriginalException(e);
		}

		long startupIn = System.currentTimeMillis() - time;
		log.debug("Startup done in: "+startupIn+" ms");

	}

}
