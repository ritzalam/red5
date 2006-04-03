package org.red5.server;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.core.NestedRuntimeException;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright © 2006 by respective authors (see below). All rights reserved.
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
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Dominick Accattato (Dominick@gmail.com)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */

/**
 * Entry point from which the server config file is loaded
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Dominick Accattato (Dominick@gmail.com)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 * @version 0.3
 */
public class Standalone {
	
	// Initialize Logging
	protected static Log log =
        LogFactory.getLog(Standalone.class.getName());
	
	protected static String red5ConfigPath = "./conf/red5.xml";
	
	/**
	 * Main entry point for the Red5 Server 
	 * usage java Standalone
	 * @param args String passed in that points to a red5.xml config file
	 * @return void
	 */
	public static void main(String[] args) throws Exception {
		
		if (args.length == 1) {
			red5ConfigPath = args[0];
		}
		
		// Detect root of Red5 configuration and set as system property
		File fp = new File(red5ConfigPath);
		fp = fp.getCanonicalFile();
		String root = fp.getAbsolutePath();
		root = root.replace("\\", "/");
		int idx = root.lastIndexOf('/');
		root = root.substring(0, idx);
		System.setProperty("red5.config_root", root);

		// Explicitly setup logging
		PropertyConfigurator.configure(root + "/log4j.properties");
		
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
		System.setProperty("red5.root", root);

		if (log.isInfoEnabled()){ 
			log.info("RED5 Server (http://www.osflash.org/red5)");
			log.info("Loading Spring Application Context: "+red5ConfigPath);
		}
		
		// Spring Loads the xml config file which initializes 
		// beans and loads the server
		FileSystemXmlApplicationContext appCtx = null;
		try {
			appCtx = new FileSystemXmlApplicationContext(red5ConfigPath);
		} catch (Exception e) {
			if (e instanceof InvocationTargetException)
				log.error("Could not start Red5.", ((InvocationTargetException) e).getCause());
			else if (e instanceof NestedRuntimeException) {
				Throwable cause = ((NestedRuntimeException) e).getCause();
				if (cause.getCause() != null)
					cause = cause.getCause();
				log.error("Could not start Red5.", cause);
			} else
				log.error("Could not start Red5.", e);
			
			return;
		}
		if(log.isDebugEnabled()) {
			log.debug("Startup date: "+appCtx.getStartupDate());
		}

	}

}
