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

import org.red5.logging.Red5LoggerFactory;
import org.red5.server.api.Red5;
import org.slf4j.Logger;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.context.support.FileSystemXmlApplicationContext;

/**
 * Launches Red5.
 *
 * @author The Red5 Project (red5@osflash.org)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class Launcher {
		
	/**
	 * Launch Red5 under it's own classloader
	 */
	public void launch() {
		try {	
			//dumpClassLoaderNames();
			
			System.out.println("Root: " + System.getProperty("red5.root"));
			System.out.println("Deploy type: " + System.getProperty("red5.deployment.type"));
			System.out.println("Logback selector: " + System.getProperty("logback.ContextSelector"));
			
			//install the slf4j bridge (mostly for JUL logging)
			SLF4JBridgeHandler.install();
			//we create the logger here so that it is instanced inside the expected 
			//classloader
			Logger log = Red5LoggerFactory.getLogger(Launcher.class);
		    //version info banner
			log.info("{} (http://www.osflash.org/red5)", Red5.getVersion());
			//pimp red5
			System.out.printf("%s (http://www.osflash.org/red5)\n", Red5.getVersion());
			
			//create red5 app context
			FileSystemXmlApplicationContext ctx = new FileSystemXmlApplicationContext(new String[]{"classpath:/red5.xml"}, false);	
			//refresh must be called before accessing the bean factory
			ctx.refresh();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	private void dumpClassLoaderNames() {
		ClassLoader tcl = Thread.currentThread().getContextClassLoader();		
		System.out.printf("[Launcher] Classloaders:\nSystem %s\nParent %s\nThis class %s\nTCL %s\n\n", ClassLoader.getSystemClassLoader(), tcl.getParent(), Launcher.class.getClassLoader(), tcl);
	}	
	*/
}
